package cz.minarik.nasapp.data.db.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DatabaseError
import com.prof.rssparser.Parser
import cz.minarik.base.data.NetworkState
import cz.minarik.base.di.base.BaseRepository
import cz.minarik.nasapp.R
import cz.minarik.nasapp.data.db.dao.RSSSourceDao
import cz.minarik.nasapp.data.db.entity.RSSSourceEntity
import cz.minarik.nasapp.utils.RealtimeDatabaseHelper
import cz.minarik.nasapp.utils.RealtimeDatabaseQueryListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class RSSSourceRepository(
    private val context: Context,
    private val dao: RSSSourceDao,
) : BaseRepository(),
    RealtimeDatabaseQueryListener<List<RealtimeDatabaseHelper.RssFeedDTO>> {

    companion object {
        const val cacheExpirationMillis = 1000L * 60L * 60L * 24 // 1 day
    }

    val state = MutableLiveData<NetworkState>()

    fun updateRSSSourcesFromRealtimeDB() {
        //todo minFetchGap
        state.postValue(NetworkState.LOADING)
        RealtimeDatabaseHelper.getNewsFeeds(this)
    }

    private fun updateDB(allFromServer: MutableList<RealtimeDatabaseHelper.RssFeedDTO>) {

        val parser = Parser.Builder()
            .context(context)
            .charset(Charset.forName("UTF-8"))
            .cacheExpirationMillis(cacheExpirationMillis)
            .build()

        GlobalScope.launch {
            val allDB = dao.getNonUserAdded()

            val dbUrls = allDB.map { it.url }
            val allServerUrls = allFromServer.map { it.url }

            //delete entries no longer present on server
            for (dbUrl in dbUrls) {
                if (!allServerUrls.contains(dbUrl)) {
                    allDB.find { it.url == dbUrl }?.let {
                        dao.delete(it)
                    }
                }
            }

            //create or update existing
            for (feed in allFromServer) {
                feed.url?.let {
                    try {
                        val channel = parser.getChannel(it)

                        var entity = dao.getByUrl(it)

                        if (entity == null) {
                            entity = RSSSourceEntity(
                                url = it,
                                title = channel.title,
                                imageUrl = channel.image?.url
                            )
                        }

                        dao.insert(entity)
                    } catch (e: Exception) {
                        //todo timber and handle exception
                        state.postValue(NetworkState.error(e.message))
                    }
                }
            }
            state.postValue(NetworkState.SUCCESS)
        }
    }

    override fun onDataChange(data: List<RealtimeDatabaseHelper.RssFeedDTO>?) {
        val allRssFeeds = mutableListOf<RealtimeDatabaseHelper.RssFeedDTO>()
        data?.let {
            allRssFeeds.addAll(data)
        }
        updateDB(allRssFeeds)
    }

    override fun onCancelled(error: DatabaseError) {
        //todo timber
        state.postValue(NetworkState.Companion.error(context.getString(R.string.common_base_error)))
    }

    suspend fun setSelected(selectedUrl: String?) {
        for (source in dao.getAll()) {
            source.isSelected = source.url == selectedUrl
            dao.update(source)
        }
    }

}
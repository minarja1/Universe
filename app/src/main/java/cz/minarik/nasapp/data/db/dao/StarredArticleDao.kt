package cz.minarik.nasapp.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import cz.minarik.base.data.db.dao.BaseDao
import cz.minarik.nasapp.data.db.entity.RSSSourceEntity
import cz.minarik.nasapp.data.db.entity.ReadArticleEntity
import cz.minarik.nasapp.data.db.entity.StarredArticleEntity

@Dao
interface StarredArticleDao : BaseDao<StarredArticleEntity> {

    @Query("Select * From StarredArticleEntity")
    suspend fun getAll(): List<StarredArticleEntity>

    @Query("Select * From StarredArticleEntity Where guid = :guid")
    suspend fun getByGuid(guid: String): StarredArticleEntity?

    @Query("Select * From StarredArticleEntity Where sourceUrl = :sourceUrl")
    suspend fun getBySourceUrl(sourceUrl: String): List<StarredArticleEntity>
}
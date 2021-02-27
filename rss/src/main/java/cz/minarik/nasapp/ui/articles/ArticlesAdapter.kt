package cz.minarik.nasapp.ui.articles

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import cz.minarik.nasapp.R
import cz.minarik.nasapp.base.BaseListAdapterNew
import cz.minarik.nasapp.ui.custom.ArticleDTO
import cz.minarik.nasapp.ui.custom.ArticleListItemView
import kotlinx.android.synthetic.main.article_list_item.view.*

class ArticlesAdapter(
    private var onItemClicked: (imageView: ImageView, titleTextView: TextView, position: Int) -> Unit,
    private var onItemLongClicked: (position: Int) -> Unit,
    private var onItemExpanded: (position: Int) -> Unit,
    private var articleShown: (article: ArticleDTO) -> Unit,
    private var filterBySource: (url: String?) -> Unit,
) : BaseListAdapterNew<ArticleDTO>(
    R.layout.row_article_item,
    diffCallback,
) {
    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ArticleDTO>() {

            override fun areItemsTheSame(oldItem: ArticleDTO, newItem: ArticleDTO): Boolean {
                return oldItem.guid == newItem.guid
            }

            override fun areContentsTheSame(oldItem: ArticleDTO, newItem: ArticleDTO): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun getItemAtPosition(position: Int): ArticleDTO? {
        return getItem(position)
    }

    override fun bind(
        itemView: View,
        item: ArticleDTO,
        position: Int,
        viewHolder: BaseViewHolderImp
    ) {
        val articleItemView = itemView.findViewById<ArticleListItemView>(R.id.articleListItem)
        articleItemView.set(item)
        articleItemView.setOnClickListener {
            onItemClicked(
                if (item.expanded) articleItemView.articleFullImageView else articleItemView.articleImageView,
                articleItemView.titleTextView,
                viewHolder.adapterPosition
            )
        }
        articleItemView.setOnLongClickListener {
            onItemLongClicked(viewHolder.adapterPosition)
            true
        }
        articleItemView.onItemExpanded = {
            onItemExpanded.invoke(position)
        }
        articleItemView.filterBySource = {
            filterBySource.invoke(it)
        }
    }

    override fun itemOnScreen(item: ArticleDTO) {
        super.itemOnScreen(item)
        articleShown.invoke(item)
    }
}
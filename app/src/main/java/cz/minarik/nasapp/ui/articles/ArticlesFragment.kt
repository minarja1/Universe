package cz.minarik.nasapp.ui.articles

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import coil.load
import cz.minarik.base.common.extensions.showToast
import cz.minarik.base.common.extensions.tint
import cz.minarik.base.data.NetworkState
import cz.minarik.base.data.Status
import cz.minarik.nasapp.R
import cz.minarik.nasapp.ui.articles.source_selection.SourceSelectionFragment
import cz.minarik.nasapp.ui.articles.source_selection.SourceSelectionViewModel
import cz.minarik.nasapp.ui.custom.ArticleDTO
import cz.minarik.nasapp.utils.isScrolledToTop
import cz.minarik.nasapp.utils.scrollToTop
import cz.minarik.nasapp.utils.sharedGraphViewModel
import cz.minarik.nasapp.utils.toFreshLiveData
import kotlinx.android.synthetic.main.fragment_articles.*
import kotlinx.android.synthetic.main.include_toolbar_with_subtitle.*
import org.koin.android.ext.android.inject


class ArticlesFragment : GenericArticlesFragment(R.layout.fragment_articles) {

    private val sourcesViewModel: SourceSelectionViewModel by inject()

    override val viewModel by sharedGraphViewModel<ArticlesFragmentViewModel>(R.id.articles_nav_graph)

    private val viewState = ViewState()

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else if (articlesRecyclerView?.isScrolledToTop() == true) {
                if (doubleBackToExitPressedOnce) {
                    requireActivity().finish()
                } else {
                    doubleBackToExitPressedOnce = true;
                    showToast(requireContext(), getString(R.string.press_back_again_to_leave))

                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)

                }
            } else {
                articlesRecyclerView?.scrollToTop()
                appBarLayout.setExpanded(true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initObserve()
        initSwipeToRefresh()
    }

    override fun showError(error: String?) {
        //todo
    }

    override fun showLoading(show: Boolean) {
        //todo
    }

    override fun initViews(view: View?) {
        super.initViews(view)
        setupDrawerNavigation()
        articlesRecyclerView?.let {
            stateView.attacheContentView(it)
        }
    }


    private fun setupDrawerNavigation() {
        drawerLayout?.let {
            val toggle = ActionBarDrawerToggle(
                activity, it, toolbar, 0, 0
            )
            it.addDrawerListener(toggle)
            toggle.syncState()
        }
        toolbar?.navigationIcon?.tint(requireContext(), R.color.colorOnBackground)

        val fragmentManager = childFragmentManager
        fragmentManager.executePendingTransactions()
        val transaction = fragmentManager.beginTransaction()
        transaction.let {
            it.replace(R.id.nav_view_content, SourceSelectionFragment())
            it.commit()
        }
    }

    override fun initObserve() {
        super.initObserve()

        viewModel.articles.observe {
            viewState.articles = it
            if (viewModel.shouldScrollToTop) {
                appBarLayout.setExpanded(true)
            }
        }

        viewModel.state.observe {
            //todo drzet si v NetworkState i exception a rozlisovat noInternet od jinych
            viewState.loadingArticlesState = it
        }


        sourcesViewModel.selectedSource.toFreshLiveData().observe {
            viewModel.loadArticles(scrollToTop = true)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        sourcesViewModel.selectedSourceName.observe {
            toolbarSubtitleContainer.isVisible = !it.isNullOrEmpty()
            toolbarSubtitle.text = it
        }
        sourcesViewModel.selectedSourceImage.observe {
            toolbarImageView.load(it)
        }

        sourcesViewModel.sourceRepository.state.toFreshLiveData().observe {
            if (it == NetworkState.SUCCESS) {
                sourcesViewModel.updateSources()
            }
            viewState.loadingSourcesState = it
        }
        sourcesViewModel.sourceRepository.sourcesChanged.toFreshLiveData().observe {
            if (it) viewModel.loadArticles()
        }

        sourcesViewModel.sourceRepository.state.toFreshLiveData().observe {
            if (it == NetworkState.SUCCESS) {
                sourcesViewModel.updateSources()
            }
            viewState.loadingSourcesState = it
        }
    }

    private fun initSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadArticles(true)
        }
    }

    override fun navigateToArticleDetail(extras: FragmentNavigator.Extras, articleDTO: ArticleDTO) {
        val action =
            ArticlesFragmentDirections.actionArticlesToArticleDetail(articleDTO)
        findNavController().navigate(action, extras)
    }

    private fun updateViews() {
        val loadingArticles = viewState.loadingArticlesState == NetworkState.LOADING
        val loadingSources = viewState.loadingSourcesState == NetworkState.LOADING
        val loading = loadingArticles || loadingSources
        val isError = viewState.loadingArticlesState?.status == Status.FAILED
        val articlesEmpty = viewState.articles.isEmpty()
        val loadingMessage = viewState.loadingArticlesState?.message

        val showShimmer = loading && articlesEmpty && !isError
        shimmerLayout.isVisible = showShimmer

        val showLoadingSwipeRefresh = loading && !showShimmer && !isError
        swipeRefreshLayout.isRefreshing = showLoadingSwipeRefresh
        swipeRefreshLayout.isEnabled = !showShimmer

        if (articlesEmpty && !isError && !loading) {
            stateView.empty(true)
        } else if (isError) {
            if (articlesEmpty) {
                //full-screen error
                stateView.error(show = true, message = loadingMessage) {
                    viewModel.loadArticles()
                }
            } else {
                showToast(requireContext(), loadingMessage ?: getString(R.string.common_base_error))
                stateView.error(false)
            }
        } else {
            //hide stateView
            stateView.loading(false)
        }
    }

    inner class ViewState {
        var loadingArticlesState: NetworkState? = null
            set(value) {
                field = value
                updateViews()
            }
        var loadingSourcesState: NetworkState? = null
            set(value) {
                field = value
                updateViews()
            }
        var articles: List<ArticleDTO> = emptyList()
            set(value) {
                field = value
                updateViews()
            }
    }
}

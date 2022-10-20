package com.cubing.snapcubs2.ui.story

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.storyapp.R
import com.cubing.snapcubs2.helper.MainViewModel
import com.cubing.snapcubs2.helper.StoryViewModelFactory
import com.cubing.snapcubs2.network.ApiService
import com.cubing.snapcubs2.network.ListStoryItem
import com.cubing.snapcubs2.network.TokenPreference
import com.cubing.snapcubs2.ui.addStory.AddStoryActivity
import com.cubing.snapcubs2.ui.auth.LoginActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StoryActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var rvStories: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var mTokenPreference: TokenPreference
    private lateinit var mainViewModel: MainViewModel
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var token: String
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var IS_MAPS: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        rvStories = findViewById(R.id.rv_stories)
        fabAdd = findViewById(R.id.fab_add)

        val manager = supportFragmentManager
        val ft: FragmentTransaction = manager.beginTransaction()
        mapFragment = manager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (IS_MAPS) {
            rvStories.visibility = View.GONE
            ft.show(mapFragment)
            ft.commit()
        } else {
            rvStories.visibility = View.VISIBLE
            ft.hide(mapFragment)
            ft.commit()
        }

        showLoading(true)
        setupViewModel()
        setupView()
        setupList()

        fabAdd.setOnClickListener {
            val addStoryIntent = Intent(this@StoryActivity, AddStoryActivity::class.java)
            startActivity(addStoryIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        var listStories = mutableListOf<ListStoryItem>()
        var builder = LatLngBounds.Builder()

        lifecycleScope.launch {
            storyAdapter.loadStateFlow
                .collect {
                    listStories.addAll(storyAdapter.snapshot().items)
                    Log.d("listStories", listStories.toString())

                    listStories.forEachIndexed { _, it ->
                        mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(it.lat, it.lon))
                                .title(it.name)
                                .snippet(it.description)
                        )
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(it.lat, it.lon)))
                        builder.include(LatLng(it.lat, it.lon))
                    }
                }

            var bounds: LatLngBounds = builder.build()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30))
            mMap.setInfoWindowAdapter(CustomInfoWindow(this@StoryActivity))
        }
    }

    private fun setupViewModel() {
        mTokenPreference = TokenPreference(this@StoryActivity)
        token = mTokenPreference.getToken()
        val factory = StoryViewModelFactory(ApiService(), token)
        mainViewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)
        showLoading(true)
    }

    private fun setupView() {
        storyAdapter = StoryAdapter()
        rvStories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = storyAdapter.withLoadStateFooter(
                footer = LoadingStateAdapter {
                    storyAdapter.retry()
                }
            )
            setHasFixedSize(true)
        }
        showLoading(true)
    }

    private fun setupList() {
        lifecycleScope.launch {
            mainViewModel.quote.collectLatest { pagedData ->
                storyAdapter.submitData(pagedData)
            }
        }
        showLoading(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_logout -> {
                val settings1: SharedPreferences =
                    this@StoryActivity.getSharedPreferences("token_pref", Context.MODE_PRIVATE)
                settings1.edit().clear().apply()

                val settings2: SharedPreferences =
                    this@StoryActivity.getSharedPreferences("session_pref", Context.MODE_PRIVATE)
                settings2.edit().clear().apply()

                finish()

                startActivity(Intent(this, LoginActivity::class.java))
            }
            R.id.menu_switch -> {
                val manager = supportFragmentManager
                val ft: FragmentTransaction = manager.beginTransaction()

                if (!IS_MAPS) {
                    item.setIcon(R.drawable.ic_baseline_menu_24)
                    IS_MAPS = true
                    rvStories.visibility = View.GONE
                    ft.show(mapFragment)
                    ft.commit()
                } else {
                    item.setIcon(R.drawable.ic_baseline_location_on_24)
                    IS_MAPS = false
                    rvStories.visibility = View.VISIBLE
                    ft.hide(mapFragment)
                    ft.commit()
                }
            }
        }
        return true
    }

    private fun showLoading(isLoading: Boolean) {
        val pgBar: ProgressBar = findViewById(R.id.progress_bar)

        if (isLoading) {
            pgBar.visibility = View.VISIBLE
        } else {
            pgBar.visibility = View.GONE
        }
    }
}
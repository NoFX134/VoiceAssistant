package ru.myproject.voiceassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SimpleAdapter
import ru.myproject.voiceassistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var podsAdapter: SimpleAdapter
    private val pods = mutableListOf(
        HashMap<String, String>().apply {
            put("Title", "Title1")
            put("Content", "Content1")
        },
        HashMap<String, String>().apply {
            put("Title", "Title2")
            put("Content", "Content2")
        },
        HashMap<String, String>().apply {
            put("Title", "Title3")
            put("Content", "Content3")
        },
        HashMap<String, String>().apply {
            put("Title", "Title4")
            put("Content", "Content4")
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

    }

    private fun init() {
        setSupportActionBar(binding.toolbar)
        podsAdapter = SimpleAdapter(
            applicationContext,
            pods,
            R.layout.item_pod,
            arrayOf("Title","Content"),
            intArrayOf(R.id.title, R.id.content)
        )
        binding.podsList.adapter = podsAdapter
        binding.voiceInputButton.setOnClickListener {
            Log.d(TAG, "FAB")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_stop -> {
                Log.d(TAG, "actions_stop")
                return true
            }
            R.id.action_clear -> {
                Log.d(TAG, "actions_clear")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
package ru.myproject.voiceassistant

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.SimpleAdapter
import com.google.android.material.snackbar.Snackbar
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.myproject.voiceassistant.databinding.ActivityMainBinding
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var podsAdapter: SimpleAdapter
    private lateinit var waEngine: WAEngine
    private val pods = mutableListOf<HashMap<String, String>>()
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false
    private val VOICE_RECOGNITION_REQUEST_CODE: Int = 777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        initWolframEngine()
        initTts()

    }

    private fun init() {
        setSupportActionBar(binding.toolbar)
        binding.textInputEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                val question = binding.textInputEdit.text.toString()
                askWolfram(question)
            }
            return@setOnEditorActionListener false
        }
        podsAdapter = SimpleAdapter(
            applicationContext,
            pods,
            R.layout.item_pod,
            arrayOf("Title", "Content"),
            intArrayOf(R.id.title, R.id.content)
        )
        binding.podsList.adapter = podsAdapter
        binding.podsList.setOnItemClickListener { parent, view, position, id ->
            if (isTtsReady) {
                val title = pods[position]["Title"]
                val content = pods[position]["Content"]
                textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, title)
            }
        }
        binding.voiceInputButton.setOnClickListener {
          pods.clear()
            podsAdapter.notifyDataSetChanged()
            if(isTtsReady){
                textToSpeech.stop()
            }
            showVoiceInputDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_stop -> {
                if (isTtsReady) {
                    textToSpeech.stop()
                }
                return true
            }
            R.id.action_clear -> {
                binding.textInputEdit.text?.clear()
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initWolframEngine() {
        waEngine = WAEngine().apply {
            appID = "7KYKJA-78LYL8A3HU"
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction(android.R.string.ok) {
                    dismiss()
                }
                show()
            }
    }

    private fun askWolfram(request: String) {
        binding.progresBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val query = waEngine.createQuery().apply { input = request }
            kotlin.runCatching {
                waEngine.performQuery(query)
            }.onSuccess { result ->
                withContext(Dispatchers.Main) {
                    binding.progresBar.visibility = View.GONE
                    if (result.isError) {
                        showSnackbar(result.errorMessage)
                        return@withContext
                    }
                    if (!result.isSuccess) {
                        binding.textInputEdit.error = getString(R.string.error_do_not_understand)
                        return@withContext
                    }
                    for (pod in result.pods) {
                        if (pod.isError) continue
                        val content = StringBuilder()
                        for (subpod in pod.subpods) {
                            for (element in subpod.contents) {
                                if (element is WAPlainText) {
                                    content.append(element.text)
                                }
                            }
                        }
                        pods.add(0, HashMap<String, String>().apply {
                            put("Title", pod.title)
                            put("Content", content.toString())
                        })
                    }
                    podsAdapter.notifyDataSetChanged()
                }
            }.onFailure { t ->
                withContext(Dispatchers.Main) {
                    binding.progresBar.visibility = View.GONE
                    showSnackbar(t.message ?: getString(R.string.error_something_went_wrong))
                }
            }
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(this) { code ->
            if (code != TextToSpeech.SUCCESS) {
                Log.e(TAG, "Error code: $code")
                showSnackbar(getString(R.string.error_tts_is_not_ready))
            } else {
                isTtsReady = true
            }
        }
        textToSpeech.language = Locale.US
    }

    private fun showVoiceInputDialog() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.request_hint))
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }
        runCatching {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        }.onFailure { t ->t.message?: getString(R.string.error_voice_recognition_unavailable) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==VOICE_RECOGNITION_REQUEST_CODE&&resultCode== RESULT_OK)
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.let{
                question->
                binding.textInputEdit.setText(question)
                askWolfram(question)
            }
    }
}
package tm.toma

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

val PREFS_NAME: String = "configs"

val PREFS_WORK_DURATION: String = "workDuration"

val PREFS_BREAK_DURATION: String = "breakDuration"

val FLASH: String = "flash"

class SettingsActivity : AppCompatActivity(), Loggable {

    private val sConfigs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val sEditor: SharedPreferences.Editor? by lazy { sConfigs.edit() }

    private val sWorkDurationEditText: EditText by lazy {
        findViewById(R.id.settingWorkDurationEditText) as EditText }

    private val sBreakDurationEditText: EditText by lazy {
        findViewById(R.id.settingBreakDurationEditText) as EditText }

    private val sToolbar: Toolbar by lazy { findViewById(R.id.my_toolbar) as Toolbar }

    override fun onCreate(savedInstanceState: Bundle?) {
        val flash: String? = intent.getStringExtra(FLASH)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(sToolbar)

        if (flash != null) Toast.makeText(this, flash, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        sWorkDurationEditText.setText(
                sConfigs.getInt(PREFS_WORK_DURATION, 45).toString(), TextView.BufferType.EDITABLE)
        sBreakDurationEditText.setText(
                sConfigs.getInt(PREFS_BREAK_DURATION, 5).toString(), TextView.BufferType.EDITABLE)
    }

    override fun onPause() {
        super.onPause()
        try {
            sEditor?.
                    putInt(PREFS_WORK_DURATION, sWorkDurationEditText.text.toString().toInt())?.
                    putInt(PREFS_BREAK_DURATION, sBreakDurationEditText.text.toString().toInt())?.
                    commit()
        } catch (e: NumberFormatException) {
            startActivity(
                    Intent(this, this.javaClass)
                            .putExtra(FLASH, getString(R.string.exception_minutes_must_be_int)))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_done   -> {
                startActivity(Intent(this, MainActivity::class.java)); true
            } else -> super.onOptionsItemSelected(item)
        }
    }
}

package tm.toma

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.RelativeLayout

class MainActivity : AppCompatActivity() {

    private val mActivityMainLayout: RelativeLayout by lazy {
        findViewById(R.id.activity_main) as RelativeLayout}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}

package com.example.teamproject

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.teamproject.calendar.CalendarFragment
import com.example.teamproject.databinding.ActivityMainBinding
import com.example.teamproject.myprofile.Profile
import com.example.teamproject.stopwatch.*
import com.example.teamproject.video.VideoItemFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val REQUEST_VOICE = 100

    lateinit var stopWatchService: StopWatchService
    var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("stopwatch","onServiceDisconnected")
            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("stopwatch","onServiceConnected")
            val binder = service as StopWatchService.Mybinder
            stopWatchService = binder.getService()
            val str=intent.getStringExtra("timerExpire")
            if(str!=null){
                if (str == "te"){
                    if (binding.bottomNavi.selectedItemId != R.id.bottom_stop_watch){
                        binding.bottomNavi.selectedItemId = R.id.bottom_stop_watch
                        if (stopWatchService.isRunning){
                            stopWatchViewModel.isRunning.value = true
                            stopWatchService.isTimerStarted = false
                        }
                    }
                }
            }
        }
    }
    private val stopWatchViewModel : StopWatchViewModel by viewModels()
    private val exeNameDbHelper = ExeNameDbHelper(this)

    private val mainWatchFragment by lazy { MainWatchFragment() }
    private val testFragment by lazy { TestFragment() }
    private val videoItemFragment by lazy { VideoItemFragment() }
    private val ProfileFragment by lazy { Profile() }
    private val CalendarFragment by lazy { CalendarFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("mainactivity","onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        fetchExeList()
        requestPermission()//stt 기능을 위해 RECORD_AUDIO 권한 요청
        initService()
        bottomNaviInit()
        //binding.button.setOnClickListener {
        //    val intent = Intent(this, Example::class.java)
        //    startActivity(intent) /// db 예시 화면으로 갑니다.
        //}
    }

    private fun fetchExeList() {
        val db = FirebaseFirestore.getInstance().collection("ExeList")
        val exeNameDbHelper = ExeNameDbHelper(this)
        db.get().addOnSuccessListener {
            for (doc in it){
                exeNameDbHelper.insertName(doc.id)
            }
        }
    }

    private fun initService(){
        val intent = Intent(this@MainActivity,StopWatchService::class.java)
        bindService(intent,connection, Context.BIND_AUTO_CREATE)
    }

    private fun bottomNaviInit() {//각자 기능 프래그먼트 만들어서 리플레이스 프레그먼트 함수 호출해주세요
        //메뉴 이름이나 메뉴 아이디, 아이콘 같은건 bottom_navigation_menu.xml에서 바꿔주세요
        binding.bottomNavi.setSelectedItemId(R.id.bottom_one);
        replaceFragment(CalendarFragment)
        binding.bottomNavi.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.bottom_one->{
                    replaceFragment(CalendarFragment)
                    return@setOnNavigationItemSelectedListener true }
                R.id.bottom_two->{
                    replaceFragment(videoItemFragment)
                    return@setOnNavigationItemSelectedListener true }
                R.id.bottom_three->{
                    replaceFragment(ProfileFragment)
                    return@setOnNavigationItemSelectedListener true }
                R.id.bottom_stop_watch->{
                    replaceFragment(mainWatchFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                else ->
                {return@setOnNavigationItemSelectedListener true }
            }
        }
    }

    private fun replaceFragment(fragment : Fragment) {
        val fragmentManager = supportFragmentManager.beginTransaction()
        fragmentManager.replace(R.id.main_root_layout,fragment).commit()
    }

    private fun requestPermission() {
        if(ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.RECORD_AUDIO),REQUEST_VOICE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_VOICE->{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this@MainActivity, "RECORD_AUDIO 권한 수락 완료", Toast.LENGTH_SHORT).show()
                }else{
                    requestPermission()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("mainactivity","onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.d("mainactivity","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("mainactivity","onDestroy")
        if(bound){
            stopWatchService.unbindService(connection)
        }
    }
}
package com.example.simplereminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.simplereminder.db.AppDatabase
import com.example.simplereminder.db.ReminderData
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

const val GEOFENCE_RADIUS = 200
const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days
const val GEOFENCE_DWELL_DELAY =  5 * 1000 // 5 secs
const val GEOFENCE_LOCATION_REQUEST_CODE = 12345
const val CAMERA_ZOOM_LEVEL = 13f
const val LOCATION_REQUEST_CODE = 123

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
        val username = intent.getStringExtra("username")

        findViewById<Button>(R.id.submitLocBtn).setOnClickListener {
            startActivity(
                Intent(applicationContext, MainActivity::class.java)
                    .putExtra("username", username)
            )
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun promptPermissions() {
        val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (!isLocationPermissionGranted()) {
            promptPermissions()
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                promptPermissions()
                return
            }
            this.map.isMyLocationEnabled = true

            // Zoom to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    with(map) {
                        val latLng = LatLng(it.latitude, it.longitude)
                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL))
                    }
                } else {
                    with(map) {
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(65.058, 25.472),
                                CAMERA_ZOOM_LEVEL
                            )
                        )
                    }
                }
            }
        }
        setLongClick(map)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setLongClick(map: GoogleMap) {

        map.setOnMapLongClickListener { latlng ->
            map.addMarker(
                    MarkerOptions().position(latlng)
                            .title("Reminder location")
            ).showInfoWindow()
            map.addCircle(
                    CircleOptions()
                            .center(latlng)
                            .strokeColor(Color.argb(50, 70, 70, 70))
                            .fillColor(Color.argb(70, 150, 150, 150))
                            .radius(GEOFENCE_RADIUS.toDouble())
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_LEVEL))
            createGeoFence(latlng, geofencingClient)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGeoFence(location: LatLng, geofencingClient: GeofencingClient) {
        val username = intent.getStringExtra("username")
        val messageContent = intent.getStringExtra("messageContent")
        val timeSelected = intent.getStringExtra("timeSelected")

        val geofence = Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
                .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                .build()

        val geofenceRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

        // Formatting the reminder object
        val reminderInputs = ReminderData(
                null,
                message = messageContent!!,
                location_x = location.latitude.toFloat(),
                location_y = location.longitude.toFloat(),
                reminder_time = timeSelected!!,
                creation_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                creator_id = username.toString(),
                reminder_seen = false
        )

        val reminderCalendar = GregorianCalendar.getInstance()
        val dateFormat = "dd.MM.yyyy HH:mm"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = DateTimeFormatter.ofPattern(dateFormat)
            val date = LocalDateTime.parse(reminderInputs.reminder_time, formatter)

            reminderCalendar.set(Calendar.YEAR, date.year)
            reminderCalendar.set(Calendar.MONTH, date.monthValue - 1)
            reminderCalendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            reminderCalendar.set(Calendar.HOUR_OF_DAY, date.hour)
            reminderCalendar.set(Calendar.MINUTE, date.minute)

        } else {
            if (dateFormat.contains(":")) {
                val dateparts = reminderInputs.reminder_time.split(" ").toTypedArray()[0].split(".").toTypedArray()
                val timeparts = reminderInputs.reminder_time.split(" ").toTypedArray()[1].split(":").toTypedArray()

                reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
                reminderCalendar.set(Calendar.HOUR_OF_DAY, timeparts[0].toInt())
                reminderCalendar.set(Calendar.MINUTE, timeparts[1].toInt())

            } else {
                val dateparts = reminderInputs.reminder_time.split(".").toTypedArray()
                reminderCalendar.set(Calendar.YEAR, dateparts[2].toInt())
                reminderCalendar.set(Calendar.MONTH, dateparts[1].toInt() - 1)
                reminderCalendar.set(Calendar.DAY_OF_MONTH, dateparts[0].toInt())
            }
        }
        // Save values in the database
        AsyncTask.execute {
            val database = Room.databaseBuilder(
                    this,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
            ).build()
            val uuid = database.reminderDao().insert(reminderInputs).toInt()
            database.close()
        }
        //if (reminderCalendar.timeInMillis > Calendar.getInstance().timeInMillis) {
        val intent = Intent(this, GeofenceReceiver::class.java)
                .putExtra("message", reminderInputs.message)
                .putExtra("coordinates", "${location.latitude}, ${location.longitude}")
                .putExtra("timing", timeSelected)

        val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                            applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        GEOFENCE_LOCATION_REQUEST_CODE
                )
            } else {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            }
        } else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
        }
        //val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        //alarmManager.setExact(AlarmManager.RTC, reminderCalendar.timeInMillis, pendingIntent)
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminderCalendar.timeInMillis, 10, pendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "This application needs background location to work on Android 10 and higher",
                    Toast.LENGTH_SHORT
                ).show()
                promptPermissions()
            }
        }
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() && (
                        grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                                grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                map.isMyLocationEnabled = true
                onMapReady(map)
            } else {
                Toast.makeText(
                    this,
                    "The app needs location permission to function",
                    Toast.LENGTH_LONG
                ).show()
                promptPermissions()
            }
        }
    }

    companion object {
        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }


        fun showNotification(context: Context?, message: String) {
            val CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL"
            var notificationId = 1589
            notificationId += Random(notificationId).nextInt(1, 30)

            val notificationBuilder = NotificationCompat.Builder(context!!.applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_assignment_turned_in_white_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}
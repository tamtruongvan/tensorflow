package org.tensorflow.lite.examples.objectdetection

import Data.Version
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class UpdateActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var btnDownload: Button
    private lateinit var btnBackHome:Button
    private lateinit var downloadManager:DownloadManager
    private lateinit var lblDownLoadStatus:TextView
    private var downloadReference:Long = 0
    private var isDownloadCompleted:Boolean = false
    private lateinit var _url:String
    private lateinit var _version:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)
        this.progressBar = findViewById(R.id.progressBar)
        this.btnDownload = findViewById(R.id.btn_Download)
        this.btnBackHome = findViewById(R.id.btn_BackHome)
        this.lblDownLoadStatus=findViewById(R.id.lb_downloadStatus)
        _url= intent.getStringExtra("updateUrl").toString()
        _version=intent.getStringExtra("version").toString()
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registerReceiver(onNotificationClick, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))

        this.btnDownload?.setOnClickListener {
            Utils.DeleteModelFile(applicationContext,getString(R.string.ModelName))
            startDownload(Uri.parse(_url))
        }
        this.btnBackHome?.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
        }
    }



    private fun startDownload(uri: Uri): Long {

        lblDownLoadStatus.text="Update is processing"
        btnDownload.visibility= View.GONE
        val request = DownloadManager.Request(uri)

        // Setting title of request
        request.setTitle("Data Download")

        // Setting description of request
        request.setDescription("Android Data download using DownloadManager.")

        // Set the local destination for the downloaded file to a path
        // within the application's external files directory
        request.setDestinationInExternalFilesDir(this.applicationContext, "", this.applicationContext.getString(R.string.ModelName))
        // Enqueue download and save into referenceId
        downloadReference = downloadManager?.enqueue(request) ?: -1

        btnDownload.isEnabled = false
        return downloadReference
    }
    fun getStatusMessage(downloadId: Long): String {

        val query = DownloadManager.Query()
        // set the query filter to our previously Enqueued download
        query.setFilterById(downloadId)

        // Query the download manager about downloads that have been requested.
        val cursor = downloadManager?.query(query)
        if (cursor?.moveToFirst() == true) {
            return downloadStatus(cursor)
        }
        return "NO_STATUS_INFO"
    }

    fun downloadStatus(cursor: Cursor): String {

        // column for download  status
        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(columnIndex)
        // column for reason code if the download failed or paused
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)

        var statusText = ""
        var reasonText = ""

        when (status) {
            DownloadManager.STATUS_FAILED -> {
                statusText = "STATUS_FAILED"
                when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> reasonText = "ERROR_CANNOT_RESUME"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> reasonText = "ERROR_DEVICE_NOT_FOUND"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> reasonText = "ERROR_FILE_ALREADY_EXISTS"
                    DownloadManager.ERROR_FILE_ERROR -> reasonText = "ERROR_FILE_ERROR"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> reasonText = "ERROR_HTTP_DATA_ERROR"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> reasonText = "ERROR_INSUFFICIENT_SPACE"
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> reasonText = "ERROR_TOO_MANY_REDIRECTS"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> reasonText = "ERROR_UNHANDLED_HTTP_CODE"
                    DownloadManager.ERROR_UNKNOWN -> reasonText = "ERROR_UNKNOWN"
                }
            }
            DownloadManager.STATUS_PAUSED -> {
                statusText = "STATUS_PAUSED"
                when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> reasonText = "PAUSED_QUEUED_FOR_WIFI"
                    DownloadManager.PAUSED_UNKNOWN -> reasonText = "PAUSED_UNKNOWN"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> reasonText = "PAUSED_WAITING_FOR_NETWORK"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> reasonText = "PAUSED_WAITING_TO_RETRY"
                }
            }
            DownloadManager.STATUS_PENDING -> statusText = "STATUS_PENDING"
            DownloadManager.STATUS_RUNNING -> statusText = "STATUS_RUNNING"
            DownloadManager.STATUS_SUCCESSFUL -> statusText = "Update completed"
        }

        return "Download Status: $statusText, $reasonText"
    }
    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isDownloadCompleted = true
            lblDownLoadStatus.text = getStatusMessage(downloadReference)
            btnBackHome.visibility=View.VISIBLE

            // update new version info to local
            var version=Version(_version,_url)
            val jsonVerson=Gson().toJson(version)
            Utils.WriteVersonFile(context,context.getString(R.string.VersionFile),jsonVerson)
            Toast.makeText(context, R.string.toast_download_completed, Toast.LENGTH_LONG).show()
            val intent=Intent()
            intent.setAction(UpdateHelper.ACTION_UPDATE_COMPLETE)
            context.sendBroadcast(intent)
        }
    }

    private var onNotificationClick: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // The download notification was clicked
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
        unregisterReceiver(onNotificationClick)
    }
}
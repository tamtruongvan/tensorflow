package org.tensorflow.lite.examples.objectdetection

import Data.Version
import Utils.ReadFileTask
import Utils.getJsonDataFromAsset
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.Image
import android.net.ConnectivityManager
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.CONNECTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

class UpdateHelper(val context:Context,val updateListener: UpdateEventListener) {
    companion object {
        val ACTION_UPDATE_COMPLETE = "tamtruong.com.action.UPDATE_COMPLETE"
    }

    fun checkUpdateInfo(){
        //return Version("2.0","url");
        val url:String= context.getString(R.string.VersionUrl)
        Thread(Runnable{
            var rf= ReadFileTask();
            rf.getTextFromWeb(url)
            val versionType = object : TypeToken<Version>() {}.type
            var updateInfo:Version=Gson().fromJson(rf.getContent(),versionType)
            var curVersion=getCurrentVersion()

            if(curVersion.Version < updateInfo.Version){
                updateListener?.onCheckUpdate(updateInfo)
            }
        }).start()
    }
    fun getCurrentVersion(): Version {
        val json= getJsonDataFromAsset(context,context.getString(R.string.VersionFile))
        val versionType = object : TypeToken<Version>() {}.type
        return Gson().fromJson(json,versionType)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun downloadFile(url: URL, fileName: String) {
        url.openStream().use { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.copy(it, Paths.get(fileName))
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        }
    }

}

interface UpdateEventListener{
    fun onCheckUpdate(updateVersion:Version)
}
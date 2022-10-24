/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.*
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import kotlin.math.max


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs),TextToSpeech.OnInitListener {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var tts: TextToSpeech? = null
    private var scaleFactor: Float = 1f

    private var bounds = Rect()
    private var boxSelectedPaint=Paint()
    private var selectedBounds=Rect()
    private var _firstPoint: PointF?=null
    private var _secondPoint: PointF?=null
    private var _scaleRateMainAndCamera: Float?=null
    private var _screenWidth:Int = 0
    private var _screenHeight:Int = 0
    private var _top:Int?=null

    init {
        initPaints()
        tts = TextToSpeech(context, this)




    }


    fun setScaleFullScreen(screenWidth:Int, screenHeight: Int){
        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the selected boundary display in Overlay.
        _screenWidth=screenWidth
        _screenHeight=screenHeight

    }
    fun getScaleOfScreen():Float{
        if(_scaleRateMainAndCamera == null)
            _scaleRateMainAndCamera = max( width* 1f / _screenWidth, height * 1f / _screenHeight)
        return _scaleRateMainAndCamera as Float
    }
    fun getTTop(): Int {
        if(_top==null){
            val rectf = Rect()
            getGlobalVisibleRect(rectf)
            _top = rectf.top
        }
        return _top as Int
    }
    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        boxSelectedPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_selectec_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        boxSelectedPaint.color = ContextCompat.getColor(context!!,R.color.bounding_box_selectec_color)
        boxSelectedPaint.strokeWidth=8F
        boxSelectedPaint.style= Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        //draw user selected boundary
        drawSelectedRec(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)

            // speech the text
            //speakOut(result)
        }
    }

    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }
    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }
    fun speakOut(detection: Detection ){
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(detection.categories[0].label, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Thread.sleep(1000)
    }

    fun drawSelectedRec(canvas: Canvas){
        if(_firstPoint==null || _secondPoint==null)return

        val top = _firstPoint!!.y - getTTop()
        val bottom = _secondPoint!!.y - getTTop()
        val left = _firstPoint!!.x
        val right = _secondPoint!!.x

        // Draw bounding box around detected objects
        val drawableRect = RectF(left, top, right, bottom)
        canvas.drawRect(drawableRect, boxSelectedPaint)

        // Create text to display alongside detected objects
        /*val drawableText =
            result.categories[0].label + " " +
                    String.format("%.2f", result.categories[0].score)*/

        // Draw rect behind display text
        /*textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()
        canvas.drawRect(
            left,
            top,
            left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
            top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )*/

        // Draw text for detected object
//        canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val curPoint= PointF(event!!.rawX,event!!.rawY)
        //get point for draw boxselectedboundary
        if(_firstPoint==null || _secondPoint==null){
            if(_firstPoint==null){
                _firstPoint=curPoint
            }
            else{
                _secondPoint=curPoint
                //swap _firstPoint and _secondPoint so that first always is top-lef and second always is bottom-right
                if(_firstPoint!!.x > _secondPoint!!.x)
                {
                    val temp=_firstPoint
                    _firstPoint=_secondPoint
                    _secondPoint=temp
                }
            }
        }else{//had boxboundary
            //check if current point not in selectedBoundary then clear boundary
            if((_firstPoint!!.x<=curPoint.x && curPoint.x<=_secondPoint!!.x) && (_firstPoint!!.y<=curPoint.y && curPoint.y<_secondPoint!!.y)){
                Toast.makeText(context, R.string.Clear_Boundary, Toast.LENGTH_LONG).show()
            }else{
                _firstPoint=null
                _secondPoint=null
            }

        }

        return super.onTouchEvent(event)
    }
}

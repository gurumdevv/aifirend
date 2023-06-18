package com.capstone.aifirend

import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import kotlin.concurrent.thread
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.capstone.aifirend.API_KEY.MY_API_KEY
import org.json.JSONArray


class ChatBackup : AppCompatActivity() {
    // Chat Box Area
    // 뷰 등록부
    private var my_text_param: ViewGroup.LayoutParams? = null
    private var avatar_text_param: ViewGroup.LayoutParams? = null
    private var chat_box_layout: LinearLayout? = null
    private var chat_box_edit_text: EditText? = null
    private var chat_box_scroll_view: ScrollView? = null
    private var chat_box_voice_button: Button? = null
    private var chat_box_voice_send_button: Button? = null
    private var chat_box_voice_cancel_button: Button? = null
    private var chat_box_amplitude_imageview: ImageView? = null
    private var chat_box_switch_button: Button? = null
    private var instance_my_text: TextView? = null
    private var instance_avatar_text: TextView? = null
    private var instance_chat_layout: LinearLayout? = null
    private var my_camera_layout: LinearLayout? = null
    private var avatar_image: ImageView? = null
    private var avatar_emotion_image: ImageView? = null
    private var my_avatar_layout: FrameLayout? = null
    // 재사용 될 뷰
    private var my_text_view: TextView? = null
    private var avatar_text_view: TextView? = null
    // 내부 컨트롤 변수
    private var density = 0f
    private var waitRespone = false
    private var isRecording = false
    private var recordPermission = false
    private var recordDuration = 0f
    private var amplitudeCanvas: Canvas? = null
    var linePaint: Paint? = Paint()
    // 보이스 레코더 & 플레이어
    private var voiceRecorder: MediaRecorder? = null
    private var voicePlayer: MediaPlayer? = null
    // 카메라
    private var camera: Camera? = null
    private var cameraPreview: CameraPreview? = null
    private var picktureCallback: Camera.PictureCallback? = null
    private var currentFace: ByteArray? = null
    private var reqeustFace: ByteArray? = null
    // 애니메이션
    private var myTextAnimation: Animation? = null
    private var avatarTextAnimation: Animation? = null
    // GPT OKHTTP Client
    private val client = OkHttpClient()
    // End Chat Box Area

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_chat)

        // Chat Box Area
        // 뷰 등록부
        chat_box_layout = findViewById(R.id.chat_box_layout)
        chat_box_edit_text = findViewById(R.id.chat_box_edit_text)
        chat_box_scroll_view = findViewById(R.id.chat_box_scroll_view)
        chat_box_voice_button = findViewById(R.id.chat_box_voice_button)
        chat_box_voice_send_button = findViewById(R.id.chat_box_voice_send_button)
        chat_box_voice_cancel_button = findViewById(R.id.chat_box_voice_cancel_button)
        chat_box_amplitude_imageview = findViewById(R.id.chat_box_amplitude_imageview)
        chat_box_switch_button = findViewById(R.id.chat_box_switch_button)
        instance_my_text = findViewById(R.id.instance_my_text)
        instance_avatar_text = findViewById(R.id.instance_avatar_text)
        instance_chat_layout = findViewById(R.id.instance_chat_layout)
        my_camera_layout = findViewById(R.id.my_camera_layout)
        avatar_image = findViewById(R.id.avatar_image)
        avatar_emotion_image = findViewById(R.id.avatar_emotion_image)
        my_avatar_layout = findViewById(R.id.my_avatar_layout)

        // 뷰 애트리뷰트
        density = resources.displayMetrics.density
        my_text_param = findViewById<TextView>(R.id.my_text).layoutParams
        (my_text_param as LinearLayout.LayoutParams).setMargins((120 * density).toInt(), (5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt())
        avatar_text_param = findViewById<TextView>(R.id.avatar_text).layoutParams
        (avatar_text_param as LinearLayout.LayoutParams).setMargins((5 * density).toInt(), (5 * density).toInt(), (120 * density).toInt(), (5 * density).toInt())

        // 뷰 초기화
        chat_box_layout?.removeAllViewsInLayout()
        var bitmap: Bitmap? = Bitmap.createBitmap(800, 150, Bitmap.Config.ARGB_8888)
        amplitudeCanvas = Canvas(bitmap!!)
        chat_box_amplitude_imageview?.setImageBitmap(bitmap!!)
        amplitudeCanvas?.drawColor(Color.parseColor("#f5f5dc"))
        linePaint?.setColor(Color.parseColor("#3366DD"))
        linePaint?.strokeWidth = 2f

        // 뷰 이벤트 등록부
        findViewById<Button>(R.id.chat_box_send_button).setOnClickListener {
            if (chat_box_edit_text!!.text.toString().trim() != "")
            {
                SendMessage(chat_box_edit_text?.text.toString(), true)
            }
        }
        chat_box_voice_button?.setOnClickListener {
            RecordVoice()
        }
        chat_box_voice_send_button?.setOnClickListener {
            SendVoice()
        }
        chat_box_voice_cancel_button?.setOnClickListener {
            CancelVoice()
        }
        chat_box_switch_button?.setOnClickListener {
            SwitchChatBox()
        }
        // End Chat Box Area
        
        // 권한 요청 호출
        RequestAudioPermission()

        // 애니메이션 등록
        myTextAnimation = AnimationUtils.loadAnimation(this, R.anim.chat_alpha_animation)
        myTextAnimation?.setAnimationListener(object: AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }
            override fun onAnimationStart(animation: Animation?) {

            }
            override fun onAnimationEnd(animation: Animation?) {
                instance_my_text?.visibility = View.GONE
            }
        })
        avatarTextAnimation = AnimationUtils.loadAnimation(this, R.anim.chat_alpha_animation)
        avatarTextAnimation?.setAnimationListener(object: AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }
            override fun onAnimationStart(animation: Animation?) {

            }
            override fun onAnimationEnd(animation: Animation?) {
                instance_avatar_text?.visibility = View.GONE
                avatar_image?.setImageResource(R.drawable.chara)
                avatar_emotion_image?.setImageResource(R.drawable.alpha)
            }
        })
    }

    // 음성 녹음 메소드
    private fun RecordVoice()
    {
        // 이미 녹음 중이라면 리턴 (2중 클릭 등 방지)
        // 응답을 기다리고 있는 중이라도 리턴
        if (waitRespone)
        {
            Toast.makeText(this, "이전 메세지가 처리 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isRecording) return
        // 녹음 권한이 없다면 토스트 출력 후 리턴
        if (!recordPermission)
        {
            Toast.makeText(this, "음성 녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try
        {
            // 음성 녹음 시작
            recordDuration = 0f

            voiceRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(cacheDir.path + "/temp_send.aac")
                prepare()
            }
            voiceRecorder?.start()
            // 진폭 캔버스 초기화
            amplitudeCanvas?.drawColor(Color.parseColor("#f5f5dc"))

            // 음성 녹음 버튼 스위칭
            SwitchVoiceState()

            // 음성 녹음 타이머 시작 (최대 30초 녹음)
            StartVoiceRecordTimer()
        }
        catch (e: java.lang.Exception)
        {
            Toast.makeText(this, "음성 대화 시작에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 음성 전송 메소드
    private fun SendVoice()
    {
        // 음성 녹음 중이 아니거나 녹음 시간이 0.5초 미만이면 리턴
        // 음성 녹음 시작 후 바로 종료 시 crash 발생 방지
        if (!isRecording || recordDuration < 0.5f) return

        // 음성 녹음 중지
        voiceRecorder?.run {
            stop()
            release()
        }
        voiceRecorder = null

        // 인풋박스 초기화
        chat_box_edit_text?.setText("")

        // 전송 텍스트 응답 대기 객체
        var new_text = TextView(this)
        new_text.layoutParams = my_text_param
        new_text.setText("음성 메세지 전송 중 ...")

        new_text.setBackgroundResource(R.drawable.my_text_drawable)
        new_text.setTextColor(Color.BLACK)
        new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

        chat_box_layout?.addView(new_text)
        my_text_view = new_text
        instance_my_text?.setText("음성 메세지 전송 중 ...")
        instance_my_text?.visibility = View.VISIBLE

        // 음성 녹음 버튼 스위칭
        SwitchVoiceState()

        // 감정 캡쳐
        TakePicture()

        // 답변이 돌아오기 전까지 다시 입력 방지
        waitRespone = true

        // NAVER API - STT

        thread(start = true) {
            var clientId: String = "vy0wukvn2f"
            var clientSecret: String = "nF1DqR50Cxv2mlNhEtnFg9hWJQdoTKVt3B5YxacH"

            try
            {
                var url: URL = URL("https://naveropenapi.apigw.ntruss.com/recog/v1/stt?lang=Kor")
                var conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                conn.useCaches = true
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId)
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret)

                var outputStream: OutputStream = conn.outputStream
                var inputStream: InputStream = FileInputStream(cacheDir.path + "/temp_send.aac")
                var buffer = ByteArray(4096)
                var read = -1
                while (true)
                {
                    read = inputStream.read(buffer)
                    if (read == -1)
                    {
                        break
                    }
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
                inputStream.close()
                var bufferedReader: BufferedReader? = null
                var responseCode = conn.responseCode

                waitRespone = false

                if (responseCode == 200)
                {
                    // 정상 호출 성공
                    bufferedReader = BufferedReader(InputStreamReader(conn.inputStream))

                    var inputLine: String? = null
                    if (bufferedReader != null)
                    {
                        var response: StringBuffer = StringBuffer()
                        while (true)
                        {
                            inputLine = bufferedReader.readLine()
                            if (inputLine == null)
                            {
                                break
                            }
                            response.append(inputLine)
                        }
                        bufferedReader.close()
                        var json: JSONObject = JSONObject(response.toString())
                        runOnUiThread {
                            if (json.getString("text").trim() == "")
                            {
                                FailMessage("인식된 음성이 없습니다 !!")
                            }
                            else
                            {
                                SendMessage(json.getString("text"), false)
                            }
                        }
                    }
                }
                else if (responseCode == 400)
                {
                    waitRespone = false
                    // 오류 발생 코드에 따른 처리
                    runOnUiThread {
                        FailMessage("음성 입력은 한국어만 지원합니다 !")
                    }
                }
                else
                {
                    waitRespone = false
                    // 오류 발생 코드에 따른 처리
                    runOnUiThread {
                        FailMessage("음성 입력에 실패했습니다.")
                    }
                }
            }
            catch (e: java.lang.Exception)
            {
                waitRespone = false
                runOnUiThread {
                    FailMessage("음성 전송에 실패했습니다.")
                }
            }

            // END NAVER API
        }

        // TEST TEMP
        //RecieveVoice("TEMP")
    }
    
    // 음성 취소 메소드
    private fun CancelVoice()
    {
        // 음성 녹음 중이 아니거나 녹음 시간이 0.5초 미만이면 리턴
        // 음성 녹음 시작 후 바로 종료 시 crash 발생 방지
        if (!isRecording || recordDuration < 0.5f) return

        // 음성 녹음 중지
        voiceRecorder?.run {
            stop()
            release()
        }
        voiceRecorder = null

        // 음성 녹음 버튼 스위칭
        SwitchVoiceState()
    }
    
    // 음성 수신 메소드 / 재생
    private fun RecieveVoice(message: String)
    {
        // 이미 재생 중인 음성이 있다면 정지
        StopVoicePlayer()

        // NAVER API - TTS

        thread(start = true) {
            var emotion = FetchEmotion()

            runOnUiThread {
                when (emotion)
                {
                    0 ->
                    {
                        avatar_image?.setImageResource(R.drawable.chara)
                        avatar_emotion_image?.setImageResource(R.drawable.alpha)
                    }
                    1 ->
                    {
                        avatar_image?.setImageResource(R.drawable.sad)
                        avatar_emotion_image?.setImageResource(R.drawable.sadeft1)
                    }
                    2 ->
                    {
                        avatar_image?.setImageResource(R.drawable.happy)
                        avatar_emotion_image?.setImageResource(R.drawable.happyeft1)
                    }
                    3 ->
                    {
                        avatar_image?.setImageResource(R.drawable.angry)
                        avatar_emotion_image?.setImageResource(R.drawable.angryeft1)
                    }
                }
            }

            var clientId: String = "vy0wukvn2f"
            var clientSecret: String = "nF1DqR50Cxv2mlNhEtnFg9hWJQdoTKVt3B5YxacH"
            try
            {
                // message: "보낼 메세지"
                var url: URL = URL("https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts")
                var conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);

                var postParams: String = "speaker=vdain&volume=5&speed=0&pitch=0&format=mp3&emotion-strength=2&emotion=" + emotion + "&text=" + URLEncoder.encode(message, "UTF-8")
                conn.doOutput = true
                var write: DataOutputStream = DataOutputStream(conn.outputStream)
                write.writeBytes(postParams)
                write.flush()
                write.close()

                var responseCode = conn.responseCode
                var buffer: BufferedReader? = null

                if (responseCode == 200)
                {
                    // 정상 호출 성공
                    var inputstream: InputStream = conn.inputStream
                    var read = 0
                    var bytes = ByteArray(1024)
                    var file: File = File(cacheDir.path + "/temp_recieve.mp3")
                    file.createNewFile()
                    var outputStream: OutputStream = FileOutputStream(file)
                    while (true)
                    {
                        read = inputstream.read(bytes)
                        if (read == -1)
                        {
                            break
                        }
                        outputStream.write(bytes, 0, read);
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputstream.close()

                    voicePlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                                .setUsage(AudioAttributes.USAGE_MEDIA).build()
                        )

                        setDataSource(cacheDir.path + "/temp_recieve.mp3")
                        isLooping = false

                        playbackParams.speed = 1f
                        prepare()
                    }

                    voicePlayer?.start()
                }
                else
                {
                    // 에러 코드에 따른 처리
                    runOnUiThread {
                        Toast.makeText(this, "답변 음성 생성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            catch (e: java.lang.Exception)
            {
                runOnUiThread {
                    Toast.makeText(this, "답변 음성 생성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // END NAVER API

        // 응답 후 응답 대기 해제
        //waitRespone = false
    }

    // 음성 재생 정지
    private fun StopVoicePlayer()
    {
        if (voicePlayer != null)
        {
            voicePlayer?.release()
            voicePlayer = null
        }
    }
    
    // 음성 녹음 권한 요청
    private fun RequestAudioPermission()
    {
        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA), 201)
    }

    // 권한 요청 결과 이벤트
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode)
        {
            201 ->
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    recordPermission = true
                }
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    ReadyCamera()
                }
            }
        }
    }
    
    // 코드 중복 줄이기
    // 음성 녹음 버튼 상태
    private fun SwitchVoiceState()
    {
        if (isRecording)
        {
            chat_box_voice_button!!.visibility = View.VISIBLE
            chat_box_voice_send_button!!.visibility = View.GONE
            chat_box_voice_cancel_button!!.visibility = View.GONE
        }
        else
        {
            chat_box_voice_button!!.visibility = View.GONE
            chat_box_voice_send_button!!.visibility = View.VISIBLE
            chat_box_voice_cancel_button!!.visibility = View.VISIBLE
        }
        isRecording = !isRecording
    }

    // 채팅 박스 스위치
    private fun SwitchChatBox()
    {
        if (chat_box_scroll_view!!.visibility == View.VISIBLE)
        {
            chat_box_scroll_view!!.visibility = View.GONE
            chat_box_switch_button?.setText("채팅 펼치기")
            instance_chat_layout!!.visibility = View.VISIBLE
            /*
            var params: ViewGroup.LayoutParams = my_avatar_layout!!.layoutParams
            params.width = (250f * density).toInt()
            params.height = (250f * density).toInt()
            my_avatar_layout?.layoutParams = params
            */
        }
        else
        {
            chat_box_scroll_view!!.visibility = View.VISIBLE
            chat_box_switch_button?.setText("채팅 접기")
            instance_chat_layout!!.visibility = View.GONE
            /*
            var params: ViewGroup.LayoutParams = my_avatar_layout!!.layoutParams
            params.width = (100f * density).toInt()
            params.height = (100f * density).toInt()
            my_avatar_layout?.layoutParams = params
            */
        }
    }

    // 음성 녹음 시간 쓰레드
    private fun StartVoiceRecordTimer()
    {
        thread(start = true) {
            while (isRecording)
            {
                Thread.sleep(100)

                runOnUiThread {

                    try
                    {
                        // 텍스트로 녹음 시간 표시
                        chat_box_voice_send_button?.setText("전송하기\n${DecimalFormat("0.0").format(recordDuration)}초/30.0초")

                        // 진폭 캔버스에 녹음 상태 시각적으로 표시
                        amplitudeCanvas?.drawLine(recordDuration * 800f / 30f, 150f,
                                                   recordDuration * 800f / 30f, 150f - voiceRecorder?.maxAmplitude!!.toFloat() / Short.MAX_VALUE.toFloat() * 150f, linePaint!!)
                        //amplitude_canvas?.drawLine(record_duration * 800f / 30f, 50f,
                        //                           record_duration * 800f / 30f, 100f, line_paint!!)

                        // 30초 이상 도달 시 자동으로 전송
                        if (recordDuration >= 30)
                        {
                            SendVoice()
                        }
                    }
                    catch (e: java.lang.Exception)
                    {

                    }
                }

                recordDuration += 0.1f
            }
        }
    }

    // 메세지 전송 실패
    private fun FailMessage(message: String)
    {
        if (my_text_view != null)
        {
            my_text_view?.setText(message)
        }
        else
        {
            var new_text = TextView(this)
            new_text.layoutParams = my_text_param
            new_text.setText(message)

            new_text.setBackgroundResource(R.drawable.my_text_drawable)
            new_text.setTextColor(Color.BLACK)
            new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

            chat_box_layout?.addView(new_text)
        }
        instance_my_text?.setText(message)
        instance_my_text?.visibility = View.VISIBLE
        instance_my_text?.startAnimation(myTextAnimation)
    }

    // 메세지 전송 메소드
    private fun SendMessage(message: String, needEmotion: Boolean)
    {
        // 음답 대기 중이라면 리턴
        if (waitRespone)
        {
            Toast.makeText(this, "이전 메세지가 처리 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (chat_box_edit_text!!.length() > 100)
        {
            Toast.makeText(this, "메세지는 100자 이내로 전송해야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 감정 캡쳐
        if (needEmotion) TakePicture()

        // 송신 메세지 객체 생성
        if (my_text_view != null)
        {
            my_text_view?.setText(message)
        }
        else
        {
            var new_text = TextView(this)
            new_text.layoutParams = my_text_param
            new_text.setText(message)

            new_text.setBackgroundResource(R.drawable.my_text_drawable)
            new_text.setTextColor(Color.BLACK)
            new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

            chat_box_layout?.addView(new_text)
        }
        instance_my_text?.setText(message)
        instance_my_text?.visibility = View.VISIBLE
        instance_my_text?.startAnimation(myTextAnimation)

        // 인풋박스 초기화
        chat_box_edit_text?.setText("")

        // 응답 대기
        waitRespone = true
        // 최신 메세지로 스크롤
        CheckScroll()

        // 메세지 응답 대기 텍스트 객체
        var new_text = TextView(this)
        new_text.layoutParams = avatar_text_param
        new_text.setText("대답 대기중 ...")

        new_text.setBackgroundResource(R.drawable.avatar_text_drawable)
        new_text.setTextColor(Color.BLACK)
        new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

        chat_box_layout?.addView(new_text)
        avatar_text_view = new_text
        instance_avatar_text?.setText("대답 대기중 ...")
        instance_avatar_text?.visibility = View.VISIBLE

        avatar_image?.setImageResource(R.drawable.chara)
        avatar_emotion_image?.setImageResource(R.drawable.waiting2)

        // TEST TEMP
        // RecieveMessage("안녕하세요. 만나서 반갑습니다.")

        // 대답 생성 요청
        getResponse(message) { response ->
            runOnUiThread {
                RecieveMessage(response)
            }
        }
    }

    // 메세지 수신 메소드
    private fun RecieveMessage(message: String)
    {
        /*
        // 수신 메세지 객체 생성
        var new_text = TextView(this)
        new_text.layoutParams = avatar_text_param
        new_text.setText(message)

        new_text.setBackgroundResource(R.drawable.avatar_text_drawable)
        new_text.setTextColor(Color.BLACK)
        new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

        chat_box_layout?.addView(new_text)
        */

        // 수신 메세지 텍스트 수정
        avatar_text_view?.setText(message)
        avatar_text_view = null
        instance_avatar_text?.setText(message)
        instance_avatar_text?.startAnimation(avatarTextAnimation)

        // 최신 메세지로 스크롤
        CheckScroll()

        RecieveVoice(message)

        // 응답 후 응답 대기 해제
        waitRespone = false
    }

    // 가장 최신 메세지로 스크롤
    private fun CheckScroll()
    {
        thread(start = true) {
            Thread.sleep(100)

            runOnUiThread {
                chat_box_scroll_view?.smoothScrollTo(0, chat_box_layout!!.bottom.toInt())
            }
        }
    }

    // 감정 정보 API
    private fun FetchEmotion(): Int
    {
        if (reqeustFace == null) return 0

        var clientId: String = "vy0wukvn2f"
        var clientSecret: String = "nF1DqR50Cxv2mlNhEtnFg9hWJQdoTKVt3B5YxacH"
        try
        {
            var url: URL = URL("https://naveropenapi.apigw.ntruss.com/vision/v1/face")
            var conn: HttpURLConnection = url.openConnection() as HttpURLConnection

            conn.useCaches = false
            conn.doOutput = true
            conn.doInput = true

            var boundary = "---" + System.currentTimeMillis() + "---"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
            conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId)
            conn.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret)

            var outputStream: OutputStream = conn.outputStream
            val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)
            val LINE_FEED = "\r\n"

            writer.append("--" + boundary).append(LINE_FEED)
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"emotion.jpeg\"").append(LINE_FEED)
            writer.append("Content-Type: " + "image/jpeg").append(LINE_FEED)
            writer.append(LINE_FEED)
            writer.flush()

            Base64.encodeToString(currentFace, Base64.DEFAULT)

            outputStream.write(reqeustFace, 0, reqeustFace!!.size)
            outputStream.flush()

            writer.append(LINE_FEED).flush()
            writer.append("--" + boundary + "--").append(LINE_FEED)
            writer.close()

            var br: BufferedReader? = null
            val responseCode: Int = conn.responseCode

            if (responseCode == 200)
            {
                br = BufferedReader(InputStreamReader(conn.inputStream))
            }
            var inputLine: String?
            if (br != null)
            {
                val response = StringBuffer()
                while (true)
                {
                    inputLine = br.readLine()
                    if (inputLine == null) break
                    response.append(inputLine)
                }
                br.close()
                var json: JSONObject = JSONObject(response.toString())

                if (json.getJSONObject("info").getInt("faceCount") < 1) return 0
                when ((json.getJSONArray("faces")[0] as JSONObject).getJSONObject("emotion").getString("value"))
                {
                    "angry" ->
                    {
                        return 3
                    }
                    "disgust" ->
                    {
                        return 0
                    }
                    "fear" ->
                    {
                        return 0
                    }
                    "laugh" ->
                    {
                        return 2
                    }
                    "neutral" ->
                    {
                        return 0
                    }
                    "sad" ->
                    {
                        return 1
                    }
                    "suprise" ->
                    {
                        return 0
                    }
                    "smile" ->
                    {
                        return 2
                    }
                    "talking" ->
                    {
                        return 0
                    }
                }
            }
        }
        catch (e: java.lang.Exception)
        {
            
        }
        return 0
    }

    // 사진 촬영
    private fun TakePicture()
    {
        //camera?.takePicture(null, null, picktureCallback)
        reqeustFace = currentFace
    }

    // 카메라 사용 준비
    private fun ReadyCamera()
    {
        try
        {
            for (i: Int in 0..Camera.getNumberOfCameras())
            {
                var info: CameraInfo? = CameraInfo()
                Camera.getCameraInfo(i, info)
                if (info!!.facing == CameraInfo.CAMERA_FACING_FRONT)
                {
                    camera = Camera.open(i)
                    camera?.setDisplayOrientation(90)
                    cameraPreview = CameraPreview(this, camera!!)

                    // Set the Preview view as the content of our activity.
                    //cameraPreview?.visibility = View.GONE
                    my_camera_layout?.addView(cameraPreview)

                    camera?.setPreviewCallback(object: Camera.PreviewCallback {
                        override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
                            val params: Camera.Parameters = camera!!.getParameters()
                            val w = params.previewSize.width
                            val h = params.previewSize.height
                            val format = params.previewFormat
                            val image: YuvImage = YuvImage(data, format, w, h, null)
                            val out: ByteArrayOutputStream = ByteArrayOutputStream()
                            val area: Rect = Rect(0, 0, w, h)
                            image.compressToJpeg(area, 50, out)
                            //currentFace = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
                            currentFace = out.toByteArray()
                        }
                    })

                    break
                }
            }

            /*
            picktureCallback = Camera.PictureCallback { data, camera ->
                try
                {
                    currentFace = Base64.encodeToString(data, Base64.DEFAULT)

                    camera?.startPreview()

                    FetchEmotion()
                } 
                catch (e: Exception) {

                }
            }
            */

        }
        catch (e: Exception)
        {

        }
    }

    // 카메라 자원 해제
    override fun onDestroy() {
        super.onDestroy()

        camera?.apply {
            release()
            camera = null
        }
    }

    // GPT Request // by Edmond
    fun getResponse(question: String, callback: (String) -> Unit) {
        val url = "https://api.openai.com//v1/chat/completions"

        val requestBody = """
            {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "${"너는 곰돌이야. " + question + ". 친구처럼 반말로 대답해줘."}"}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header( "Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $MY_API_KEY")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API FALILED", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                /*
                if (body != null) {
                    Log.v("data", body)
                } else {
                    Log.v("data", "empty")
                }
                */

                var jsonObject = JSONObject(body)
                val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                val textResult = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                callback(textResult)

            }
        })
    }
}
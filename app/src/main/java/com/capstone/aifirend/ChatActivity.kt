package com.capstone.aifirend

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.aifirend.API_KEY.MY_API_KEY
import com.capstone.aifirend.databinding.ActivityChatBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

class ChatActivity : AppCompatActivity() {
    // Chat Box Area
    // 뷰 등록부
    //private var my_text_param: ViewGroup.LayoutParams? = null
    //private var avatar_text_param: ViewGroup.LayoutParams? = null
    //private var chat_box_layout: LinearLayout? = null
    private var chat_box_edit_text: EditText? = null
    //private var chat_box_scroll_view: ScrollView? = null
    private var chat_box_recycler_view: RecyclerView? = null
    // 재사용 될 뷰
    //private var my_text_view: TextView? = null
    //private var avatar_text_view: TextView? = null
    // 내부 컨트롤 변수
    private var density = 0f
    private var waitRespone = false
    private var messageList: ArrayList<MessageRVModal> = arrayListOf()
    private var chatDAO: ChatDAO = ChatDAO(this)
    // GPT OKHTTP Client
    private val client = OkHttpClient()
    // End Chat Box Area
    private lateinit var viewContainer : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //Status bar Transparency
        viewContainer = findViewById(R.id.chat_inner_container)

        this.setStatusBarTransparent()

        viewContainer.setPadding(
            0,
            this.statusBarHeight(),
            0,
            this.navigationHeight()
        )

        // Chat Box Area
        // 뷰 등록부
        //chat_box_layout = findViewById(R.id.chat_box_layout)
        chat_box_edit_text = findViewById(R.id.chat_box_edit_text)
        //chat_box_scroll_view = findViewById(R.id.chat_box_scroll_view)
        chat_box_recycler_view = findViewById(R.id.chat_box_recycler_view)

        // 뷰 애트리뷰트
        density = resources.displayMetrics.density
        /*
        my_text_param = findViewById<TextView>(R.id.my_text).layoutParams
        (my_text_param as LinearLayout.LayoutParams).setMargins((120 * density).toInt(), (5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt())
        avatar_text_param = findViewById<TextView>(R.id.avatar_text).layoutParams
        (avatar_text_param as LinearLayout.LayoutParams).setMargins((5 * density).toInt(), (5 * density).toInt(), (120 * density).toInt(), (5 * density).toInt())
        */
        // 뷰 초기화
        //chat_box_layout?.removeAllViewsInLayout()

        // 뷰 이벤트 등록부
        findViewById<Button>(R.id.chat_box_send_button).setOnClickListener {
            var target: String = chat_box_edit_text!!.text.toString().trim()
            if (target != "")
            {
                SendMessage(target)
            }
        }
        chat_box_edit_text?.setOnEditorActionListener {view, id, event ->
            var target: String = chat_box_edit_text!!.text.toString().trim()
            if (target != "")
            {
                SendMessage(target)
            }
            true
        }
        // End Chat Box Area

        // 채팅 DB
        FetchChatHistory()

        // 리사이클러 뷰 채팅
        chat_box_recycler_view!!.layoutManager = LinearLayoutManager(this)
        chat_box_recycler_view!!.adapter = MessageRVAdapter(messageList)

        CheckScroll()
    }

    //상태바 및 네비게이션바 투명화
    fun Activity.setStatusBarTransparent() {
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        if(Build.VERSION.SDK_INT >= 30) {	// API 30 에 적용
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    fun Context.statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")

        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId)
        else 0
    }

    fun Context.navigationHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId)
        else 0
    }


    // 채팅 로그 가져오기
    private fun FetchChatHistory()
    {
        messageList = chatDAO.FetchAllChat()
    }
    
    // 메세지 전송 메소드
    private fun SendMessage(message: String)
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

        // 송신 메세지 객체 생성
        /*
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
        */
        // 송신 메세지 등록
        var item = MessageRVModal(message,"user")
        messageList.add(item)
        chatDAO.InsertChat(item)
        chat_box_recycler_view?.adapter?.notifyItemInserted(messageList.size - 1)

        // 인풋박스 초기화
        chat_box_edit_text?.setText("")

        // 응답 대기
        waitRespone = true
        // 최신 메세지로 스크롤
        CheckScroll()

        // 메세지 응답 대기 텍스트 객체
        /*
        var new_text = TextView(this)
        new_text.layoutParams = avatar_text_param
        new_text.setText("대답 대기중 ...")

        new_text.setBackgroundResource(R.drawable.avatar_text_drawable)
        new_text.setTextColor(Color.BLACK)
        new_text.setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())

        chat_box_layout?.addView(new_text)
        avatar_text_view = new_text
        */
        // 메세지 응답 대기 임시 객체
        messageList.add(MessageRVModal("대답 대기중 ...","bot"))
        chat_box_recycler_view?.adapter?.notifyItemInserted(messageList.size - 1)

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
        //avatar_text_view?.setText(message)
        //avatar_text_view = null
        // 수신 메세지 등록
        messageList[messageList.size - 1].message = message
        chatDAO.InsertChat(messageList[messageList.size - 1])
        chat_box_recycler_view?.adapter?.notifyItemChanged(messageList.size - 1)

        // 최신 메세지로 스크롤
        CheckScroll()

        // 응답 후 응답 대기 해제
        waitRespone = false
    }

    // 가장 최신 메세지로 스크롤
    private fun CheckScroll()
    {
        if (messageList.size < 1) return
        thread(start = true) {
            Thread.sleep(100)

            runOnUiThread {
                //chat_box_scroll_view?.smoothScrollTo(0, chat_box_layout!!.bottom.toInt())
                chat_box_recycler_view?.smoothScrollToPosition(messageList.size - 1)
            }
        }
    }

    // GPT Request // by Edmond
    fun getResponse(question: String, callback: (String) -> Unit) {
        val url = "https://api.openai.com//v1/chat/completions"

        val requestBody = """
            {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "${getString(R.string.additional_PARAM1) + question + getString(R.string.additional_PARAM2)}"}]
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
                callback("대답 중 문제가 발생했습니다.")
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

    override fun onDestroy() {
        chatDAO.close()
        super.onDestroy()
    }
}
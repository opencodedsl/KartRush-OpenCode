data class Notice(
    val title: String, 
    val noticeId: String,
    val date: String,
    val category: String,
    val content: String = ""
)

suspend fun Nothing_Type(int: Int, type: String = "title", category: String = "[공지사항]"): String {
    var apiUrl = "";
    return try {
        withContext(Dispatchers.IO) {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("accept", "application/json")
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                reader.use { it.forEachLine { line -> response.append(line) } }
                val jsonResponse = JSONObject(response.toString())
                return@withContext jsonResponse.getString(type)
            } else {
                Log.e("TAG", "API 요청 실패! 응답 코드: $responseCode")
                return@withContext "test: " + category +"\nerror Code: $responseCode"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("Error", e.message.toString())
        "Exception occurred"
    }
}

suspend fun ALLNote(tap: String = ""): List<Notice> {
    val server = "https://open.api.nexon.com/kartrush/v1/"
    val url1 = server + "notice"
    val url2 = server + "notice-season"
    val url3 = server + "notice-event"
    return try {
        val note = NoteAPI(url1)
        val note1 = NoteAPI(url2, "season_notice", "[시즌 알림판]")
        val note2 = NoteAPI(url3, "event_notice", "[이벤트]")
        var combinedNotes = note + note1 + note2 + note3;
        if (tap == "공지사항"){
            combinedNotes = note;
        }else if (tap == "시즌"){
            combinedNotes = note1;
        }else if (tap == "이벤트"){
            ombinedNotes = note2;
        }
        val sortedNotes = combinedNotes.sortedByDescending { it.date }
        sortedNotes
    } catch (e: Exception) {
        emptyList<Notice>()
    }
}

suspend fun NoteAPI(
    url: String,
    type: String = "notice",
    category: String = "[공지사항]"
): List<Notice> {
    val apiKey = "api key"
    return try {
        withContext(Dispatchers.IO) {
        val url = URL(url)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("accept", "application/json")
        connection.setRequestProperty("x-nxopen-api-key", apiKey)
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            reader.use { it.forEachLine { line -> response.append(line) } }
            val jsonResponse = JSONObject(response.toString())
            val noticeArray: JSONArray = jsonResponse.getJSONArray(type)
            val noticeList = mutableListOf<Notice>()
            for (i in 0 until noticeArray.length()) {
            try {
                val noticeItem = noticeArray.getJSONObject(i)
                val noticeId = noticeItem.getString("notice_id")
                val date = noticeItem.getString("date")
                val title = noticeItem.getString("title")
                noticeList.add(Notice(title, noticeId, date, category, KartAPI().Nothing_Type(noticeId.toInt(), "contents", category)))
                } catch (e: JSONException) {
                    Log.e("TAG", "error:  $i", e)
                }
            }
            return@withContext noticeList
            } else {
                return@withContext emptyList<Notice>()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList<Notice>()
    }
}

@Composable
fun NoteDetail(title: String, note_id: String, category: String, onBack: () -> Unit, content: String, dark: Color = Color.Black) {
    var warn by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(Color.Red),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
        ) {
        val isDarkMode = dark == Color.Black  // 다크모드 여부
        val style = if (isDarkMode) {
            """
            <style>
            body {
                background-color: black;
                color: white;
                font-size: 16px;
                margin: 0;
                padding: 10px;
            }
            </style>
            """
        } else {
            """
            <style>
            body {
                background-color: white;
                color: black;
                font-size: 16px;
                margin: 0;
                padding: 10px;
            }
            </style>
            """
        }
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                settings.javaScriptEnabled = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.textZoom = 143
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?,request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                            return true
                            } catch (e: Exception) {
                                warn = "알 수 없는 이유로 이동할 수 없습니다.";
                                return false
                            }
                        }
                        return false
                    }
                }
            }
        },
        update = { webView ->
            val regex = """/embed/([a-zA-Z0-9_-]{11})""".toRegex()
            val matchResult = regex.find(content)
            val videoId = matchResult?.groupValues?.get(1)
            val content = if (videoId != null) {
            val cleanIframe = """
            <iframe 
                src="https://www.youtube.com/embed/$videoId" 
                frameborder="0" 
                allowfullscreen="true">
            </iframe>
            """.trimIndent()
            content.replace("""<div class="embeddedContent[^>]*>.*?</div>""".toRegex(), cleanIframe)
            } else {
                content
            }
            webView.loadDataWithBaseURL(
                "",
                """
                <html>
                <head> 
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    $style
                    <style>img, iframe {width: 100% !important;height: auto !important;} </style>
                </head>
                <body>
                    [ ${title} ]</br>${content}</br>${warn}
                </body>
                </html>
                """.trimIndent(),
"text/html",
                    "UTF-8",
                    null)},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp, bottom = 88.dp))
        }
    }
    Log.e("con", content)
}
package ai.flowstorm.core.servlets

import ai.flowstorm.core.handlers.google.GoogleAssistantApp
import java.io.ByteArrayOutputStream
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "Google Assistant Servlet", urlPatterns = ["/google"])
class GoogleAssistantServlet : HttpServlet() {

    val app = GoogleAssistantApp()

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        res.status = 405
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val buf = ByteArrayOutputStream()
        req.inputStream.use { it.copyTo(buf) }
        val input = buf.toString()
        val headers = req.headerNames.toList().map { it to req.getHeader(it) }.toMap()
        GoogleAssistantApp.appKey.set(req.getParameter("key"))
        val output = app.handleRequest(input, headers).get()
        res.contentType = "application/json"
        res.outputStream.println(output)
    }
}
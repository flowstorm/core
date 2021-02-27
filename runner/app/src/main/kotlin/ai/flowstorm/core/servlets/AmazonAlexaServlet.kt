package ai.flowstorm.core.servlets

import com.amazon.ask.Skills
import com.amazon.ask.servlet.SkillServlet
import ai.flowstorm.core.handlers.alexa.*
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Amazon Alexa Servlet", urlPatterns = ["/alexa"])
class AmazonAlexaServlet : SkillServlet(
        Skills.standard()
            .addRequestHandlers(
                    CancelAndStopIntentHandler(),
                    MessageIntentHandler(),
                    HelpIntentHandler(),
                    LaunchRequestHandler(),
                    SessionEndedRequestHandler())
            .build()
)
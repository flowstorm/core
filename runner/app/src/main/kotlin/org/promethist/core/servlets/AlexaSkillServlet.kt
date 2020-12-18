package org.promethist.core.servlets

import com.amazon.ask.Skills
import com.amazon.ask.servlet.SkillServlet
import org.promethist.core.handlers.alexa.*
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Alexa Skill Servlet", urlPatterns = ["/alexa"])
class AlexaSkillServlet : SkillServlet(
        Skills.standard()
            .addRequestHandlers(
                    CancelAndStopIntentHandler(),
                    MessageIntentHandler(),
                    HelpIntentHandler(),
                    LaunchRequestHandler(),
                    SessionEndedRequestHandler())
            .build()
)
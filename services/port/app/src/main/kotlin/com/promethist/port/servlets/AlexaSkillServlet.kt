package com.promethist.port.servlets

import com.amazon.ask.Skills
import com.amazon.ask.servlet.SkillServlet
import com.promethist.port.alexa.handlers.*
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
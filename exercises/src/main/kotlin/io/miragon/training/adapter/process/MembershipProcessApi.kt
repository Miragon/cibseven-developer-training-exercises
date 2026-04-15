@file:Suppress("unused")

package io.miragon.training.adapter.process

object MembershipProcessApi {

    const val PROCESS_KEY: String = "subscribeNewsletter"

    object Elements {
        const val START_EVENT_NEWSLETTER_WANTED: String = "startEvent_newsletterWanted"
        const val USER_TASK_FILL_OUT_FORM: String = "userTask_fillOutForm"
        const val SERVICE_TASK_SEND_WELCOME_MAIL: String = "serviceTask_sendWelcomeMail"
        const val END_EVENT_USER_SUBSCRIBED: String = "endEvent_userSubscribed"
    }

    object Variables {
        const val MEMBERSHIP_ID: String = "membershipId"
        const val EMAIL: String = "email"
        const val NAME: String = "name"
        const val AGE: String = "age"
    }
}

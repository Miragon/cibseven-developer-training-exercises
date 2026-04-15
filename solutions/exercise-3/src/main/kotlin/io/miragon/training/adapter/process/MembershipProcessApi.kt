@file:Suppress("unused")

package io.miragon.training.adapter.process

object MembershipProcessApi {

    const val PROCESS_KEY: String = "subscribeNewsletter"

    object Elements {
        const val START_EVENT_NEWSLETTER_WANTED: String = "startEvent_newsletterWanted"
        const val USER_TASK_FILL_OUT_FORM: String = "userTask_fillOutForm"
        const val SERVICE_TASK_CLAIM_MEMBERSHIP: String = "serviceTask_claimMembership"
        const val GATEWAY_HAS_EMPTY_SPOTS: String = "gateway_hasEmptySpots"
        const val SERVICE_TASK_SEND_CONFIRMATION_MAIL: String = "serviceTask_sendConfirmationMail"
        const val USER_TASK_CONFIRM_SUBSCRIPTION: String = "userTask_confirmSubscription"
        const val SERVICE_TASK_SEND_WELCOME_MAIL: String = "serviceTask_sendWelcomeMail"
        const val END_EVENT_USER_SUBSCRIBED: String = "endEvent_userSubscribed"
        const val SERVICE_TASK_SEND_REJECTION_MAIL: String = "serviceTask_sendRejectionMail"
        const val END_EVENT_MEMBERSHIP_REJECTED: String = "endEvent_membershipRejected"
    }

    object Variables {
        const val MEMBERSHIP_ID: String = "membershipId"
        const val EMAIL: String = "email"
        const val NAME: String = "name"
        const val AGE: String = "age"
        const val HAS_EMPTY_SPOTS: String = "hasEmptySpots"
    }
}

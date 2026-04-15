@file:Suppress("unused")

package io.miragon.training.adapter.process

object MembershipProcessApi {

    const val PROCESS_KEY: String = "subscribeNewsletter"

    object Messages {
        const val MESSAGE_CONFIRMATION_REJECTED: String = "Message_confirmationRejected"
    }

    object Signals {
        const val SIGNAL_MEMBERSHIP_ACTIVATED: String = "Signal_membershipActivated"
    }

    object Elements {
        const val START_EVENT_NEWSLETTER_WANTED: String = "startEvent_newsletterWanted"
        const val USER_TASK_FILL_OUT_FORM: String = "userTask_fillOutForm"
        const val SERVICE_TASK_CLAIM_MEMBERSHIP: String = "serviceTask_claimMembership"
        const val GATEWAY_HAS_EMPTY_SPOTS: String = "gateway_hasEmptySpots"
        const val SERVICE_TASK_SEND_CONFIRMATION_MAIL: String = "serviceTask_sendConfirmationMail"
        const val SUB_PROCESS_CONFIRM_MEMBERSHIP: String = "subProcess_confirmMembership"
        const val USER_TASK_CONFIRM_SUBSCRIPTION: String = "userTask_confirmSubscription"
        const val SERVICE_TASK_SEND_WELCOME_MAIL: String = "serviceTask_sendWelcomeMail"
        const val END_EVENT_MEMBERSHIP_ACTIVATED: String = "endEvent_membershipActivated"
        const val SERVICE_TASK_SEND_REJECTION_MAIL: String = "serviceTask_sendRejectionMail"
        const val END_EVENT_MEMBERSHIP_REJECTED: String = "endEvent_membershipRejected"
        const val SERVICE_TASK_RE_SEND_CONFIRMATION_MAIL: String = "serviceTask_reSendConfirmationMail"
        const val SERVICE_TASK_REVOKE_CLAIM: String = "serviceTask_revokeClaim"
        const val END_EVENT_MEMBERSHIP_DECLINED: String = "endEvent_membershipDeclined"
        const val START_EVENT_SIGNAL_MEMBERSHIP_ACTIVATED: String = "startEvent_signalMembershipActivated"
        const val SERVICE_TASK_NOTIFY_ABOUT_SIGNED_MEMBERSHIP: String = "serviceTask_notifyAboutSignedMembership"
        const val CALL_ACTIVITY_HANDLE_REJECTION: String = "callActivity_handleRejection"
    }

    object SubProcessKeys {
        const val HANDLE_REJECTION: String = "handleRejection"
    }

    object Variables {
        const val MEMBERSHIP_ID: String = "membershipId"
        const val EMAIL: String = "email"
        const val NAME: String = "name"
        const val AGE: String = "age"
        const val HAS_EMPTY_SPOTS: String = "hasEmptySpots"
    }
}

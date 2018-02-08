package software.aws.toolkits.jetbrains.utils

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.ProjectRule
import org.junit.Rule
import org.junit.Test

class NotificationUtilsTest {

    @Rule
    @JvmField
    val projectRule = ProjectRule()

    @Test
    fun notificationOnExceptionWithoutMessageShowsStackTrace() {
        val project = projectRule.project

        val messageBus = project.messageBus.connect()
        var notification: Notification? = null
        messageBus.setDefaultHandler { _, params ->
            notification = params[0] as Notification
        }
        messageBus.subscribe(Notifications.TOPIC)

        NullPointerException().notifyError("ooops", project = project)

        assert(notification).isNotNull {
            assert(it.actual.content) {
                startsWith("java.lang.NullPointerException")
                contains("NotificationUtilsTest.kt")
            }
        }
    }
}
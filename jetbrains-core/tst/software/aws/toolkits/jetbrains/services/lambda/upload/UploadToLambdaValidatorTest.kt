// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda.upload

import assertk.Assert
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RuleChain
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.runInEdtAndGet
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.amazon.awssdk.services.lambda.model.Runtime
import software.aws.toolkits.jetbrains.services.iam.IamRole
import software.aws.toolkits.jetbrains.utils.rules.JavaCodeInsightTestFixtureRule
import software.aws.toolkits.jetbrains.utils.rules.openClass
import javax.swing.DefaultComboBoxModel

@RunsInEdt
class UploadToLambdaValidatorTest {
    private val projectRule = JavaCodeInsightTestFixtureRule()

    @Rule
    @JvmField
    val ruleChain = RuleChain(projectRule, EdtRule())

    private val sut = UploadToLambdaValidator()
    private lateinit var view: EditFunctionPanel

    @Before
    fun wireMocksTogetherWithValidOptions() {
        view = runInEdtAndGet {
            EditFunctionPanel(projectRule.project)
        }

        view.name.text = "name"
        view.description.text = "description"
        view.handler.text = "com.example.LambdaHandler::handleRequest"
        val role = IamRole("DummyArn")
        view.iamRole.model = DefaultComboBoxModel(arrayOf(role))
        view.iamRole.selectedItem = role
        view.runtime.model = DefaultComboBoxModel(Runtime.knownValues().toTypedArray())
        view.runtime.selectedItem = Runtime.JAVA8
        val bucket = "sourceBucket"
        view.sourceBucket.model = DefaultComboBoxModel(arrayOf(bucket))
        view.sourceBucket.selectedItem = bucket
        view.timeout.text = "30"
        view.memorySize.text = "512"

        projectRule.fixture.openClass(
            """
            package com.example;

            public class LambdaHandler {
                public static void handleRequest(InputStream input, OutputStream output) { }
            }
            """
        )
    }

    @Test
    fun validFunctionReturnsNull() {
        assert(sut.validateConfigurationSettings(view)).isNull()
    }

    @Test
    fun nameMustBeSpecified() {
        view.name.text = ""
        assert(sut.validateConfigurationSettings(view)).containsMessage("Function Name must be specified")
    }

    @Test
    fun validFunctionNameLength() {
        view.name.text = "aStringThatIsGreaterThanSixtyFourCharactersInLengthAndIsThereforeInvalid"
        assert(sut.validateConfigurationSettings(view)).containsMessage("must not exceed 64 characters")
    }

    @Test
    fun validFunctionCanOnlyContainAlphanumerics() {
        view.name.text = "a string"
        assert(sut.validateConfigurationSettings(view)).containsMessage("alphanumerics")
    }

    @Test
    fun handlerCannotBeBlank() {
        view.handler.text = ""
        assert(sut.validateConfigurationSettings(view)).containsMessage("Handler must be specified")
    }

    @Test
    fun runtimeMustBeSelected() {
        view.runtime.selectedItem = null
        assert(sut.validateConfigurationSettings(view)).containsMessage("Runtime must be specified")
    }

    @Test
    fun iamRoleMustBeSelected() {
        view.iamRole.selectedItem = null
        assert(sut.validateConfigurationSettings(view)).containsMessage("IAM role must be specified")
    }

    @Test
    fun timeoutMustBeSpecified() {
        view.timeout.text = ""
        assert(sut.validateConfigurationSettings(view)).containsMessage("Timeout must be between")
    }

    @Test
    fun timeoutMustBeNumeric() {
        view.timeout.text = "foo"
        assert(sut.validateConfigurationSettings(view)).containsMessage("Timeout must be between")
    }

    @Test
    fun timeoutMustBeWithinLowerBound() {
        view.timeout.text = "0"
        assert(sut.validateConfigurationSettings(view)).containsMessage("Timeout must be between")
    }

    @Test
    fun timeoutMustBeWithinUpperBound() {
        view.timeout.text = Integer.MAX_VALUE.toString()
        assert(sut.validateConfigurationSettings(view)).containsMessage("Timeout must be between")
    }

    @Test
    fun memoryMustBeSpecified() {
        view.memorySize.text = ""
        assert(sut.validateConfigurationSettings(view)).containsMessage("Memory must be between")
    }

    @Test
    fun memoryMustBeNumeric() {
        view.memorySize.text = "foo"
        assert(sut.validateConfigurationSettings(view)).containsMessage("Memory must be between")
    }

    @Test
    fun memoryMustBeWithinLowerBound() {
        view.memorySize.text = "0"
        assert(sut.validateConfigurationSettings(view)).containsMessage("Memory must be between")
    }

    @Test
    fun memoryMustBeAnIncrementOf64() {
        view.memorySize.text = "13"
        assert(sut.validateConfigurationSettings(view)).containsMessage("Memory must be between")
    }

    @Test
    fun memoryMustBeWithinUpperBound() {
        view.memorySize.text = Integer.MAX_VALUE.toString()
        assert(sut.validateConfigurationSettings(view)).containsMessage("Memory must be between")
    }

    @Test
    fun sourceBucketMustBeSelectedToDeploy() {
        view.sourceBucket.selectedItem = null
        assert(sut.validateCodeSettings(projectRule.project, view)).containsMessage("Bucket must be specified")
    }

    @Test
    fun handlerMustBeInProjectToDeploy() {
        view.handler.text = "Foo"
        assert(sut.validateCodeSettings(projectRule.project, view)).containsMessage("Must be able to locate the handler")
    }

    @Test
    fun runtimeMustBeSupportedToDeploy() {
        view.runtime.selectedItem = Runtime.NODEJS4_3
        assert(sut.validateCodeSettings(projectRule.project, view)).containsMessage("Deploying using the runtime")
    }

    private fun Assert<ValidationInfo?>.containsMessage(expectedMessage: String) {
        assert(this.actual).isNotNull { assert(it.actual.message).contains(expectedMessage) }
    }
}
package ru.citeck.ecos.notifications.lib

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class NotificationServiceTest : NotificationServiceTestBase() {

    companion object {
        @JvmStatic
        fun getTestMultiTemplateWithComplexMatchWithPredicateParams(): List<TestParams> {
            return listOf(
                TestParams(true, "unknown"),
                TestParams(true, "conditional-0"),
                TestParams(false, "unknown"),
                TestParams(false, "conditional-0")
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun simpleTemplateTest(legacyApi: Boolean) {
        initialize(legacyApi)

        record.text = "simple-template-test"
        record.typeId = "simple-template-test-type-id"

        notificationsService.send(
            Notification.Builder()
                .templateRef(createTemplateRef("sub-template-0"))
                .record(TEST_RECORD_REF)
                .build()
        )
        assertThat(receivedCommands).hasSize(1)
        assertThat(receivedCommands[0].templatesPath).isEmpty()
        assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("sub-template-0"))
        assertThat(receivedCommands[0].isTemplateRefFinal).isEqualTo(!legacyApi)
        assertThat(receivedCommands[0].model).containsAllEntriesOf(
            mapOf(
                if (legacyApi) {
                    "_etype?id" to "emodel/type@${record.typeId}"
                } else {
                    "_type?id" to "emodel/type@${record.typeId}"
                },
                "text|presuf('','-sub-template-0')" to "simple-template-test-sub-template-0"
            )
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun multiTemplateWithoutMatchTest(legacyApi: Boolean) {
        initialize(legacyApi)

        record.typeId = "unknown-type"
        record.text = "multi-without-match"

        notificationsService.send(
            Notification.Builder()
                .templateRef(createTemplateRef("multi-template-0"))
                .record(TEST_RECORD_REF)
                .build()
        )
        assertThat(receivedCommands).hasSize(1)
        assertThat(receivedCommands[0].templatesPath).isEmpty()
        assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("multi-template-0"))
        assertThat(receivedCommands[0].isTemplateRefFinal).isEqualTo(!legacyApi)
        assertThat(receivedCommands[0].model).containsAllEntriesOf(
            mapOf(
                if (legacyApi) {
                    "_etype?id" to "emodel/type@${record.typeId}"
                } else {
                    "_type?id" to "emodel/type@${record.typeId}"
                },
                "text|presuf('','-multi-att')" to "multi-without-match-multi-att"
            )
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testMultiTemplateWithSimpleMatchWithoutPredicate(legacyApi: Boolean) {

        initialize(legacyApi)

        record.typeId = "type-0"
        record.text = "type-0-txt"

        notificationsService.send(
            Notification.Builder()
                .templateRef(createTemplateRef("multi-template-0"))
                .record(TEST_RECORD_REF)
                .build()
        )
        assertThat(receivedCommands).hasSize(1)
        if (legacyApi) {
            assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("multi-template-0"))
            assertThat(receivedCommands[0].templatesPath).isEmpty()
        } else {
            assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("sub-template-0"))
            assertThat(receivedCommands[0].templatesPath).containsExactly(createTemplateRef("multi-template-0"))
        }
        assertThat(receivedCommands[0].isTemplateRefFinal).isEqualTo(!legacyApi)

        fun prepareBaseExpectedModel(): MutableMap<String, Any?> {
            val model = HashMap<String, Any?>()
            if (legacyApi) {
                model["_etype?id"] = "emodel/type@${record.typeId}"
            } else {
                model["_type?id"] = "emodel/type@${record.typeId}"
            }
            model["text|presuf('','-multi-att')"] = record.text + "-multi-att"
            return model
        }
        val expectedModel = prepareBaseExpectedModel()
        expectedModel["text|presuf('','-sub-template-0')"] = record.text + "-sub-template-0"
        if (legacyApi) {
            expectedModel["text|presuf('','-sub-template-1')"] = record.text + "-sub-template-1"
            expectedModel["text|presuf('','-sub-cond-template-0')"] = record.text + "-sub-cond-template-0"
        }
        assertThat(receivedCommands[0].model).containsExactlyInAnyOrderEntriesOf(expectedModel)
    }

    data class TestParams(
        val legacyApi: Boolean,
        val recordText: String
    )

    @ParameterizedTest
    @MethodSource("getTestMultiTemplateWithComplexMatchWithPredicateParams")
    fun testMultiTemplateWithComplexMatchWithPredicate(params: TestParams) {

        val legacyApi = params.legacyApi
        initialize(legacyApi)

        record.typeId = "type-1"
        record.text = params.recordText

        val predicateMatch = record.text == "conditional-0"

        notificationsService.send(
            Notification.Builder()
                .templateRef(createTemplateRef("multi-template-0"))
                .record(TEST_RECORD_REF)
                .build()
        )
        assertThat(receivedCommands).hasSize(1)
        if (legacyApi) {
            assertThat(receivedCommands[0].templatesPath).isEmpty()
            assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("multi-template-0"))
        } else {
            assertThat(receivedCommands[0].templatesPath).containsExactly(createTemplateRef("multi-template-0"))
            if (predicateMatch) {
                assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("sub-template-conditional-0"))
            } else {
                assertThat(receivedCommands[0].templateRef).isEqualTo(createTemplateRef("sub-template-1"))
            }
        }
        assertThat(receivedCommands[0].isTemplateRefFinal).isEqualTo(!legacyApi)

        fun prepareBaseExpectedModel(): MutableMap<String, Any?> {
            val model = HashMap<String, Any?>()
            if (legacyApi) {
                model["_etype?id"] = "emodel/type@${record.typeId}"
            } else {
                model["_type?id"] = "emodel/type@${record.typeId}"
            }
            model["text|presuf('','-multi-att')"] = record.text + "-multi-att"
            return model
        }
        val expectedModel = prepareBaseExpectedModel()
        if (legacyApi) {
            expectedModel["text|presuf('','-sub-template-0')"] = record.text + "-sub-template-0"
            expectedModel["text|presuf('','-sub-template-1')"] = record.text + "-sub-template-1"
            expectedModel["text|presuf('','-sub-cond-template-0')"] = record.text + "-sub-cond-template-0"
        } else if (predicateMatch) {
            expectedModel["text|presuf('','-sub-cond-template-0')"] = record.text + "-sub-cond-template-0"
        } else {
            expectedModel["text|presuf('','-sub-template-1')"] = record.text + "-sub-template-1"
        }
        assertThat(receivedCommands[0].model).containsExactlyInAnyOrderEntriesOf(expectedModel)
    }
}

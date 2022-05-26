package com.example.valvereceiver

import java.util.*

object Constants {
    const val MY_TAG = "MY_TAG"
    val SERVICE_UUID: UUID = UUID.fromString("e4205d54-dd14-11ec-9d64-0242ac120002")
    val CHARACTERISTIC_SEGMENT1_UUID: UUID = UUID.fromString("f0dd21f8-dd14-11ec-9d64-0242ac120002")
    val CHARACTERISTIC_SEGMENT2_UUID: UUID = UUID.fromString("adfa76bc-dd26-11ec-9d64-0242ac120002")
    val CHARACTERISTIC_SEGMENT3_UUID: UUID = UUID.fromString("b7a1fe4c-dd26-11ec-9d64-0242ac120002")
    val CHARACTERISTIC_SEGMENT4_UUID: UUID = UUID.fromString("bca2f9f0-dd26-11ec-9d64-0242ac120002")

    val allCharacteristics = listOf(
        CHARACTERISTIC_SEGMENT1_UUID,
        CHARACTERISTIC_SEGMENT2_UUID,
        CHARACTERISTIC_SEGMENT3_UUID,
        CHARACTERISTIC_SEGMENT4_UUID
    )
    //val CLIENT_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("a2edcd0c-dd15-11ec-9d64-0242ac120002")
}
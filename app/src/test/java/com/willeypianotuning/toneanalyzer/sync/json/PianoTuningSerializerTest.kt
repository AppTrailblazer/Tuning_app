package com.willeypianotuning.toneanalyzer.sync.json

import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningMeasurements
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class PianoTuningSerializerTest {
    private lateinit var serializer: PianoTuningSerializer
    private lateinit var random: Random

    @Before
    @Throws(Exception::class)
    fun setUp() {
        serializer = PianoTuningSerializer(
            TemperamentSerializer(),
            TuningStyleSerializer()
        )
        random = Random()
    }

    @Test
    fun testSerializationWithDeserialization() {
        val originalTuning = PianoTuning(
                id = "MyTestId",
                name = "Rodger's Piano",
                make = "Rodger",
                model = "Piano 2",
                serial = UUID.randomUUID().toString(),
                measurements = PianoTuningMeasurements(
                        inharmonicity = Array(88) { DoubleArray(3) { random.nextDouble() } },
                        peakHeights = Array(88) { DoubleArray(16) { random.nextDouble() } },
                        harmonics = Array(88) { DoubleArray(10) { random.nextDouble() } },
                        bxFit = DoubleArray(88) { random.nextDouble() },
                        delta = DoubleArray(88) { random.nextDouble() },
                        fx = DoubleArray(88) { random.nextDouble() },
                ),
                temperament = Temperament.EQUAL
        )
        val serialized = serializer.toJson(originalTuning)
        val deserializedTuning = serializer.fromJson(serialized)
        assertEquals(originalTuning.name, deserializedTuning.name)
        assertEquals(originalTuning.make, deserializedTuning.make)
        assertEquals(originalTuning.model, deserializedTuning.model)
        assertEquals(originalTuning.serial, deserializedTuning.serial)
        assertArrayEquals(originalTuning.measurements.bxFit, deserializedTuning.measurements.bxFit, 1e-6)
        assertArrayEquals(originalTuning.measurements.fx, deserializedTuning.measurements.fx, 1e-6)
        assertArrayEquals(originalTuning.measurements.delta, deserializedTuning.measurements.delta, 1e-6)
        assertArrayEquals(
                originalTuning.measurements.inharmonicity.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                deserializedTuning.measurements.inharmonicity.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                1e-6
        )
        assertArrayEquals(
                originalTuning.measurements.peakHeights.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                deserializedTuning.measurements.peakHeights.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                1e-6
        )
        assertArrayEquals(
                originalTuning.measurements.harmonics.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                deserializedTuning.measurements.harmonics.map { it.toTypedArray() }.toTypedArray().flatten().toDoubleArray(),
                1e-6
        )
    }

    @Test
    fun testDeserializationOfOldFormat() {
        val json = JSONObject(PianoTuningTestData.OLD_SERIALIZED_TUNING_FORMAT)
        val deserializedTuning = serializer.fromJson(json)
        assertEquals("toneanalyzer1514842772822", deserializedTuning.id)
        assertEquals("Williams", deserializedTuning.name)
        assertEquals("Hardman", deserializedTuning.make)
    }


}
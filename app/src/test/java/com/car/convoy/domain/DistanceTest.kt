package com.car.convoy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC R11-13 distanceMeters 예시표 4행. 허용 오차 ±0.01m. */
class DistanceTest {

    private val origin = Destination("기준", "", 37.5, 127.0)

    private fun at(latitude: Double) = Destination("비교", "", latitude, 127.0)

    private fun check(latitude: Double, expectedMeters: Double) =
        assertEquals(expectedMeters, distanceMeters(origin, at(latitude)), 0.01)

    @Test fun same_point_is_zero() = check(37.5, 0.0)
    @Test fun just_under_ten_meters() = check(37.50008, 8.8956)
    @Test fun just_over_ten_meters() = check(37.50009, 10.0075)
    @Test fun about_five_hundred_meters() = check(37.5045, 500.3772)

    // 부동소수점 오차로 asin 인자가 1을 넘어 NaN이 되지 않아야 한다. 대척점 거리는 πR이다.
    @Test fun antipodal_points_are_not_nan() {
        val d = distanceMeters(Destination("a", "", 0.0, 0.0), Destination("b", "", 0.0, 180.0))
        assertEquals(Math.PI * 6_371_000.0, d, 0.01)
    }

    @Test fun near_antipodal_points_are_not_nan() {
        val d = distanceMeters(Destination("a", "", 37.5, 127.0), Destination("b", "", -37.5, -53.0))
        assertEquals(Math.PI * 6_371_000.0, d, 0.01)
    }
}

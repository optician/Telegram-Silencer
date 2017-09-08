package me.optician_owl.silencer.utils

import org.scalatest.{FlatSpec, Matchers}

class HostTest extends FlatSpec with Matchers {

  behavior of "HostTest"

  it should "have correct Ordering" in {
    val host1 = Host("github.com")
    val host2 = Host("some.github.com")
    val host3 = Host("some.github.com")
    val host4 = Host("some.github.io")
    val host5 = Host("*.github.io")
    val host6 = Host("some.*.com")

    val ord = implicitly[Ordering[Host]]

    ord.compare(host1,host2) should be (-1)
    ord.compare(host2,host3) should be (0)
    ord.compare(host4,host3) should be (1)
    ord.compare(host4,host5) should be (0)
    ord.compare(host5,host6) should be (1)
    ord.compare(host3,host6) should be (0)

  }

}

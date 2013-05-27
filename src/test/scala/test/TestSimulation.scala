package test

import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.Headers.Names._
import io.gatling.http.Headers.Values._
import java.security.MessageDigest
import scala.concurrent.duration._
import bootstrap._
import assertions._


class TestSimulation extends Simulation {

  val httpProtocol = http
    .baseURL("http://localhost:8000/")
    .disableWarmUp

  val userids = csv("userids.txt").random

  // We actually use a custom saml library, but I've modified this to just create a dummy instead
  def createSamlAssertion(s: String) : String = {
    "prefix_" + s + "_suffix"
  }


  val injectSamlAssertion = (session: Session) =>  {
    session("userid").validate[String].map(userid =>
      session.setAll(Map("userid" -> userid, "saml" -> createSamlAssertion(userid))))
  }

  val scn = scenario("post userids")
    .feed(userids)
    .exec(injectSamlAssertion)
    .exec(
      http("Call service")
        .post("service")
        .headers(Map("Content-Type" -> "multipart/related; type=\"application/xop+xml\";"))
        .elFileBodyPart("nameofbodypart", "test.ssp", "application/xml" )
        .rawFileBodyPart("part2", "test.pdf", "application/pdf", "somecontentid" )

      )

  setUp(scn.inject(ramp(1 users) over (1 second))).protocols(httpProtocol)

}

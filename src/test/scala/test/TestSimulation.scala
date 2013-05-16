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

  // We actually use a custom saml library, but I've modified this to just create a createSamlAssertion instead
  def createSamlAssertion(s: String) : String = {
    new java.lang.String(MessageDigest.getInstance("MD5").digest(s.getBytes), "UTF8")
  }


  val injectSamlAssertion = (session: Session) =>  {
    session.getV[String]("userid").map(userid =>
      session.setAll(Map("userid" -> userid, "saml" -> createSamlAssertion(userid))))
  }

  val testFunc = (session: Session) => {
    println(session.get("saml"))
    println(session.get("userid"))
    session.getV[String]("saml").map(saml =>
      session.setAll(("key" ,"value"))
    )
  }

  val scn = scenario("post userids")
    .feed(userids)
    .exec(injectSamlAssertion)
    .exec(testFunc)
    .exec(
      http("Opprett dokumentbehandling")
        .post("service")
        .elFileBody("test.ssp")
      )

  setUp(scn.inject(ramp(2 users) over (2 seconds))).protocols(httpProtocol)

}

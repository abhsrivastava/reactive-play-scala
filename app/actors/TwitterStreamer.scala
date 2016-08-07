package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.iteratee.Concurrent.Broadcaster
import play.api.{Logger, Play}
import play.api.libs.iteratee._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.WS
import play.extras.iteratees.{Encoding, JsonIteratees}
import play.api.Play.current
import scala.collection.mutable.ArrayBuffer
import play.api.libs.concurrent.Execution.Implicits._

class TwitterStreamer(out: ActorRef) extends Actor {
  def receive = {
    case "subscribe" => {
      Logger.info("Received subscription from a client")
      TwitterStreamer.subscribe(out)
    }
  }

  override def postStop(): Unit = {
    Logger.info("Client Unsubsribing from stream")
    TwitterStreamer.unsubscribe(out)
  }
}

object TwitterStreamer {

  def props(out: ActorRef) = Props(new TwitterStreamer(out))
  private var broadcastEnumerator : Option[Enumerator[JsObject]] = None
  private var broadcaster : Option[Broadcaster] = None
  private var subscribers = new ArrayBuffer[ActorRef]

  def subscribe(out: ActorRef) = {
    Logger.info("came inside subsribe method")
    if (broadcastEnumerator.isEmpty) init()
    def twitterClient: Iteratee[JsObject, Unit] = Cont {
      case in@Input.EOF => Done(None)
      case in@Input.El(o) =>
        if (subscribers.contains(out)) {
          out ! o
          twitterClient
        } else {
          Done(None)
        }
      case in@Input.Empty =>
        twitterClient
    }

    broadcastEnumerator.foreach { enumerator =>
      enumerator run twitterClient
    }
    subscribers += out
    Logger.info("completed subscribe method")
  }

  def unsubscribe(out: ActorRef) = {
    Logger.info("came inside unsubscribe")
    subscribers.zipWithIndex.filter{ case(x, i) => x == out}.map{case(x, i) => subscribers.remove(i)}
  }

  def subscribeNode: Enumerator[JsObject] = {
    Logger.info("came inside subscribeNode")
    if (broadcastEnumerator.isEmpty == false) init()
    broadcastEnumerator.getOrElse(Enumerator.empty[JsObject])
  }

  def init() : Unit = {
    Logger.info("came inside init")
    credentials map { case(consumerKey, requestToken) =>
      val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
      val jsonStream: Enumerator[JsObject] = buildPipeline(enumerator)
        val (be, _) = Concurrent.broadcast(jsonStream)
        broadcastEnumerator = Some(be)
        val url = Option(System.getProperty("masterNodeUrl"))
          .getOrElse("https://stream.twitter.com/1.1/statuses/filter.json")
        WS
          .url(url)
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "java, scala, clojure, haskell")
          .get{response =>
            Logger.info("Status: " + response.status)
            iteratee
          }.map{ _ => Logger.info("stream closed")}
    } getOrElse {
      Logger.info("stream error")
    }
  }

  def buildPipeline(enumerator: Enumerator[Array[Byte]]): Enumerator[JsObject] = {
    Logger.info("came inside buildPipeline")
    val jsonStream: Enumerator[JsObject] =
      enumerator &>
        Encoding.decode() &>
        Enumeratee.grouped(JsonIteratees.jsSimpleObject)
    jsonStream
  }

  def credentials = {
    Logger.info("came inside credentials")
    for {
      apiKey <- Play.configuration.getString("twitter.apiKey")
      apiSecret <- Play.configuration.getString("twitter.apiSecret")
      token <- Play.configuration.getString("twitter.token")
      tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
    } yield(ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))
  }
}
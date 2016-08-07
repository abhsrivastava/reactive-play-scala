package controllers

/**
  * Created by abhsrivastava on 8/6/16.
  */
import actors.TwitterStreamer
import play.api.Logger
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._

class Tweets extends Controller {
  def tweets = WebSocket.acceptWithActor[String, JsValue] { implicit request => out =>
    Logger.info("came inside actor creation")
    TwitterStreamer.props(out)
  }

  def replicateFeed = Action {implicit request =>
    Logger.info("came inside replicated feed")
    Ok.feed(TwitterStreamer.subscribeNode)
  }
}

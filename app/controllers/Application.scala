package controllers

import play.api._
import play.api.mvc._

class Application extends Controller {

  def index = Action {implicit request =>
    Ok(views.html.index("Tweets"))
  }
}

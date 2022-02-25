import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal

import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

trait ClientServer extends SprayJsonSupport with DefaultJsonProtocol {

  // Data Class
  case class ErrorInfo(errorFlag: String, errorCode: Option[String], errorMessage: Option[String])
  case class Item(date: String, name_jp: String, npatients: String)
  case class ResponseData(errorInfo: ErrorInfo, itemList: List[Item])

  // Actor Setting
  implicit val system: ActorSystem = ActorSystem("Corona_API")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Marshaller
  implicit val ErrorDataMarshaller: RootJsonFormat[ErrorInfo] = jsonFormat3(ErrorInfo)
  implicit val ItemDataMarshaller: RootJsonFormat[Item] = jsonFormat3(Item)
  implicit val ResponseDataMarshaller: RootJsonFormat[ResponseData] = jsonFormat2(ResponseData)

  /**
   * Name:  sendRequest
   * Func:  Get the number of coronary cases for a given date and prefecture.
   * Args:
   *    (nameJp: String) => Prefectures in Japan.
   *    (date: String)   => Date of the data you want.
   * Return:
   *    Future[ResponseData] =>　Unmarshalled response data.
   */
  def sendRequest(nameJp: String, date: String): Future[(Query,ResponseData)] ={
    val uri = Uri("https://opendata.corona.go.jp/api/Covid19JapanAll")
    val params = Query("date" -> date, "dataName" -> nameJp)
    val request = HttpRequest(GET, uri.withQuery(params))
    Http().singleRequest(request)
      .flatMap{ response =>
        Unmarshal(response.entity).to[ResponseData]
      }
      .map(response => (params, response))
  }
}
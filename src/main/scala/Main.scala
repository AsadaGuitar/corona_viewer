import akka.http.scaladsl.model.Uri

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.annotation.tailrec

object Main extends ClientServer with Utils {

  def app(): Unit ={

    val msg =
      """都道府県と日付を半角スペース区切りで入力してください。
        |結果を表示する場合「show」と入力してください。
        |例）神奈川県 2022/02/26
        |""".stripMargin

    val regexDate = "^(19[0-9]{2}|20[0-9]{2})/(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01])"

    @tailrec
    def loop(responseList: Seq[Future[(Uri.Query,ResponseData)]], command: String): Unit = command match {
      case ""     =>
        val userCommand = io.StdIn.readLine()
        loop(responseList, userCommand)
      case "show" => Future.sequence(responseList).onComplete{
        case Success(value) => value.foreach { case (query, data) =>
          if (data.errorInfo.errorFlag.equals("1")) {
            System.err.println(s"[RequestError] Status: ${data.errorInfo.errorCode.getOrElse("Not Found")}, " +
              s"Message: ${data.errorInfo.errorMessage.getOrElse("Not Found")}")
          } else if (data.itemList.isEmpty){
            for {
              date <- query.get("date")
              name <- query.get("dataName")
            } yield {
              val dateWithHyphen = dateFormatWithHyphen(date)
              println(s"[$dateWithHyphen][$name]: データが存在しません。")
            }
          } else {
            data.itemList.foreach{ item =>
              println(s"[${item.date}][${item.name_jp}]: ${numberFormatWithComma(item.npatients)}人")
            }
          }
        }
        case Failure(error) => System.err.println(error.getMessage)
      }
      case string: String =>
        val split = string.split(" ")

        if ((split.length == 2) &&
          AdministrativeDivisionsOfJapan.exists(_.equals(split(0))) &&
          split(1).matches(regexDate)) {

          val state = split(0)
          val date  = split(1).split("/").reduce((x,acc) => x ++ acc)
          val response: Future[(Uri.Query,ResponseData)] = sendRequest(state, date)

          val userCommand = io.StdIn.readLine()
          loop(responseList :+ response, userCommand)
        } else {
          System.err.println(msg)
          val userCommand = io.StdIn.readLine()
          loop(responseList, userCommand)
        }
    }

    println(msg)
    val userCommand = io.StdIn.readLine()
    loop(Nil: Seq[Future[(Uri.Query,ResponseData)]], userCommand)
  }

  def main(args: Array[String]): Unit ={

    println(
      """*****************************
        |***** START APPLICATION *****
        |*****************************
        |""".stripMargin
    )

    app()

    io.StdIn.readLine()
    system.terminate()
      .onComplete {
        case Success(value) => value
        case Failure(error) => System.err.println(error.getMessage)
      }
  }
}

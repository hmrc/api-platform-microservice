/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatformmicroservice.common.controllers

import play.api.mvc.{PathBindable, QueryStringBindable}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment, UserId}

package object binders {

  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    ApplicationId.apply(text).toRight(s"Cannot accept $text as ApplicationId")
  }

  private def userIdFromString(text: String): Either[String, UserId] = {
    UserId.apply(text).toRight(s"Cannot accept $text as UserId")
  }

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).flatMap(applicationIdFromString)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.toString()
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApplicationId] = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      textBinder.bind(key, params).map(_.flatMap(applicationIdFromString))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      textBinder.unbind(key, applicationId.toString())
    }
  }

  implicit def environmentPathBinder(implicit textBinder: PathBindable[String]): PathBindable[Environment] = new PathBindable[Environment] {

    override def bind(key: String, value: String): Either[String, Environment] = {
      for {
        text <- textBinder.bind(key, value)
        env  <- Environment.apply(text).toRight("Not a valid environment")
      } yield env
    }

    override def unbind(key: String, env: Environment): String = {
      env.toString.toLowerCase
    }
  }

  implicit def environmentQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[Environment] = new QueryStringBindable[Environment] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Environment]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(env) => Environment.apply(env).toRight("Not a valid environment")
          case _          => Left("Unable to bind an application ID")
        }
      }
    }

    override def unbind(key: String, environment: Environment): String = {
      textBinder.unbind(key, environment.toString())
    }
  }

  implicit def userIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {

    override def bind(key: String, value: String): Either[String, UserId] = {
      textBinder.bind(key, value).flatMap(userIdFromString)
    }

    override def unbind(key: String, userId: UserId): String = {
      userId.toString
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[UserId] = new QueryStringBindable[UserId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UserId]] = {
      for {
        textOrBindError <- textBinder.bind(key, params)
      } yield textOrBindError match {
        case Right(idText) =>
          for {
            id <- UserId.apply(idText).toRight(s"Cannot accept $idText as a user identifier")
          } yield id
        case _             => Left("Unable to bind a user identifier")
      }
    }

    override def unbind(key: String, userId: UserId): String = {
      textBinder.unbind(key, userId.toString)
    }
  }

}

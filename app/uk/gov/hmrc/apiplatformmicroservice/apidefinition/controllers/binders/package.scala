/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatformmicroservice.apidefinition.controllers

import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.Environment
import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.{DeveloperIdentifier, EmailIdentifier}
import play.api.Logger

package object binders {
  import play.api.mvc.{PathBindable, QueryStringBindable}
  import uk.gov.hmrc.apiplatformmicroservice.apidefinition.models.{ApiContext, ApiVersion}

  private val logger: Logger = Logger("application")

   implicit def environmentPathBinder(implicit textBinder: PathBindable[String]): PathBindable[Environment] = new PathBindable[Environment] {
    override def bind(key: String, value: String): Either[String, Environment] = {
      for {
        text <- textBinder.bind(key, value).right
        env <- Environment.from(text).toRight("Not a valid environment").right
      } yield env
    }

    override def unbind(key: String, env: Environment): String = {
      env.toString.toLowerCase
    }
   }
  implicit def apiContextPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiContext] = new PathBindable[ApiContext] {

    override def bind(key: String, value: String): Either[String, ApiContext] = {
      textBinder.bind(key, value).map(ApiContext(_))
    }

    override def unbind(key: String, apiContext: ApiContext): String = {
      apiContext.value
    }
  }

  implicit def apiContextQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiContext] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiContext]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(context) => Right(ApiContext(context))
          case _              => Left("Unable to bind an api context")
        }
      }
    }

    override def unbind(key: String, context: ApiContext): String = {
      textBinder.unbind(key, context.value)
    }
  }

  implicit def apiVersionPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiVersion] = new PathBindable[ApiVersion] {

    override def bind(key: String, value: String): Either[String, ApiVersion] = {
      textBinder.bind(key, value).map(ApiVersion(_))
    }

    override def unbind(key: String, apiVersion: ApiVersion): String = {
      apiVersion.value
    }
  }

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiVersion] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiVersion]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(version) => Right(ApiVersion(version))
          case _              => Left("Unable to bind an api version")
        }
      }
    }

    override def unbind(key: String, version: ApiVersion): String = {
      textBinder.unbind(key, version.value)
    }
  }

  private def warnOnEmailId(id: DeveloperIdentifier): DeveloperIdentifier = id match {
    case EmailIdentifier(_) => logger.warn("Still using emails as identifier"); id
    case _ => id
  }

  implicit def developerIdentifierBinder(implicit textBinder: PathBindable[String]): PathBindable[DeveloperIdentifier] = new PathBindable[DeveloperIdentifier] {
    override def bind(key: String, value: String): Either[String, DeveloperIdentifier] = {
      for {
        text <- textBinder.bind(key, value)
        id <- DeveloperIdentifier(value).toRight(s"Cannot accept $text as a developer identifier")
        _ = warnOnEmailId(id)
      } yield id
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      DeveloperIdentifier.asText(warnOnEmailId(developerId))
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[DeveloperIdentifier] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DeveloperIdentifier]] = {
      for {
        textOrBindError <- textBinder.bind("developerId", params).orElse(textBinder.bind("email", params))
      } yield textOrBindError match {
        case Right(idText) =>
          for {
            id <- DeveloperIdentifier(idText).toRight(s"Cannot accept $idText as a developer identifier")
            _ = warnOnEmailId(id)
          } yield id
        case _ => Left("Unable to bind a developer identifier")
      }
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      textBinder.unbind("developerId", DeveloperIdentifier.asText(warnOnEmailId(developerId)))
    }
  }
}

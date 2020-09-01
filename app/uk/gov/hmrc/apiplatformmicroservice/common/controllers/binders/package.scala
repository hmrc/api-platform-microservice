/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformmicroservice.common.domain.models.{ApplicationId, Environment}

package object binders {

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).map(ApplicationId)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(appId) => Right(ApplicationId(appId))
          case _            => Left("Unable to bind an application ID")
        }
      }
    }

    override def unbind(key: String, context: ApplicationId): String = {
      textBinder.unbind(key, context.value)
    }
  }

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

  implicit def environmentQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[Environment] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Environment]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(env) => Environment.from(env).toRight("Not a valid environment")
          case _          => Left("Unable to bind an application ID")
        }
      }
    }

    override def unbind(key: String, environment: Environment): String = {
      textBinder.unbind(key, environment.entryName)
    }
  }

}

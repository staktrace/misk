package misk.aws2.sqs.jobqueue

import com.google.inject.Provides
import misk.ReadyService
import misk.ServiceModule
import misk.annotation.ExperimentalMiskApi
import misk.aws2.sqs.jobqueue.config.SqsConfig
import misk.cloud.aws.AwsRegion
import misk.inject.KAbstractModule
import misk.jobqueue.v2.JobConsumer
import misk.jobqueue.v2.JobEnqueuer
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.net.URI

@ExperimentalMiskApi
open class SqsJobQueueModule @JvmOverloads constructor(
  private val config: SqsConfig,
  private val configureClient: SqsAsyncClientBuilder.() -> Unit = {}
) : KAbstractModule() {
  override fun configure() {
    requireBinding<AwsCredentialsProvider>()
    requireBinding<AwsRegion>()
    install(ServiceModule<SqsJobConsumer>().dependsOn<ReadyService>())
    bind<JobConsumer>().to<SqsJobConsumer>()
    bind<JobEnqueuer>().to<SqsJobEnqueuer>()
  }

  @Provides
  fun sqsConfig(awsRegion: AwsRegion): SqsConfig {
    return if (config.all_queues.region != null) {
      config
    } else {
      config.copy(
        all_queues = config.all_queues.copy(
          region = awsRegion.name
        )
      )
    }
  }

  @Provides
  fun sqsClientClientFactory(
    credentialsProvider: AwsCredentialsProvider,
  ) : SqsClientFactory {
    return SqsClientFactory(credentialsProvider, configureClient)
  }
}

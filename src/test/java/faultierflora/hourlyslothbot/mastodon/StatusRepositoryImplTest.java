package faultierflora.hourlyslothbot.mastodon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import social.bigbone.MastodonClient;
import social.bigbone.MastodonRequest;
import social.bigbone.api.entity.MediaAttachment;
import social.bigbone.api.entity.Status;
import social.bigbone.api.entity.data.Visibility;
import social.bigbone.api.exception.BigBoneRequestException;
import social.bigbone.api.method.MediaMethods;
import social.bigbone.api.method.StatusMethods;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for the {@link StatusRepositoryImpl StatusRepositoryImpl}.
 * The {@link MastodonClient MastodonClient} is mocked.
 */
class StatusRepositoryImplTest {

    private final MastodonClient client = mock(MastodonClient.class);
    private final MediaMethods mediaMethods = mock(MediaMethods.class);
    private final StatusMethods statusMethods = mock(StatusMethods.class);
    @SuppressWarnings("unchecked")
    private final MastodonRequest<MediaAttachment> mediaRequest = mock(MastodonRequest.class);
    @SuppressWarnings("unchecked")
    private final MastodonRequest<Status> statusRequest = mock(MastodonRequest.class);
    private final MediaAttachment mediaAttachment = mock(MediaAttachment.class);
    private final Status status = mock(Status.class);
    private final File image = new File("00001.jpg");

    private final Map<String, Object> yaml = Map.of(
            "description", "Toe-toed sloth eating salad",
            "author", "Mathias Appel",
            "url", "https://www.flickr.com/photos/mathiasappel/25038314754",
            "license", Map.of(
                    "name", "CC0 1.0 Deed",
                    "url", "https://creativecommons.org/publicdomain/zero/1.0/"));

    private StatusRepositoryImpl repository;

    @BeforeEach
    void setUp() throws BigBoneRequestException {
        when(client.media()).thenReturn(mediaMethods);
        when(client.statuses()).thenReturn(statusMethods);
        when(mediaMethods.uploadMedia(any(File.class), anyString(), anyString())).thenReturn(mediaRequest);
        when(mediaRequest.execute()).thenReturn(mediaAttachment);
        when(mediaAttachment.getId()).thenReturn("4711");
        when(statusMethods.postStatus(anyString(), anyList(), any(Visibility.class), any(), anyBoolean(), any(), any()))
                .thenReturn(statusRequest);
        when(statusRequest.execute()).thenReturn(status);

        repository = new StatusRepositoryImpl(client);
        ReflectionTestUtils.setField(repository, "greetings", List.of("Hello everyone,"));
        ReflectionTestUtils.setField(repository, "announcement", List.of("Here is a new sloth picture for you."));
        ReflectionTestUtils.setField(repository, "salutation", "Have fun! Your hourly sloth bot");
        ReflectionTestUtils.setField(repository, "tags", List.of("sloth", "faultier"));
    }

    @Test
    void postStatusShouldUploadImageWithDescription() throws BigBoneRequestException {
        repository.postStatus(yaml, image);

        verify(mediaMethods).uploadMedia(image, "image/jpg", "Toe-toed sloth eating salad");
        verify(mediaRequest).execute();
    }

    @Test
    void postStatusShouldPostPublicEnglishStatusWithUploadedImage() throws BigBoneRequestException {
        Status result = repository.postStatus(yaml, image);

        assertThat(result).isSameAs(status);
        verify(statusMethods).postStatus(anyString(), eq(List.of("4711")), eq(Visibility.PUBLIC),
                isNull(), eq(false), isNull(), eq("en"));
        verify(statusRequest).execute();
    }

    @Test
    void postStatusShouldCreateTextWithAttributionAndHashtags() throws BigBoneRequestException {
        repository.postStatus(yaml, image);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(statusMethods).postStatus(text.capture(), anyList(), any(), any(), anyBoolean(), any(), any());
        assertThat(text.getValue()).isEqualTo("""
                Hello everyone,

                Here is a new sloth picture for you.

                This picture is from Mathias Appel and can be found here: https://www.flickr.com/photos/mathiasappel/25038314754
                It is licensed under CC0 1.0 Deed (https://creativecommons.org/publicdomain/zero/1.0/).


                Have fun! Your hourly sloth bot

                #sloth #faultier""");
    }

    @Test
    void postStatusShouldChooseGreetingAndAnnouncementFromConfiguredOptions() throws BigBoneRequestException {
        List<String> greetings = List.of("Hello everyone,", "Dear sloth fans,");
        List<String> announcements = List.of("Here is a new sloth picture for you.", "Your hourly sloth is here.");
        ReflectionTestUtils.setField(repository, "greetings", greetings);
        ReflectionTestUtils.setField(repository, "announcement", announcements);

        for (int i = 0; i < 20; i++) {
            repository.postStatus(yaml, image);
        }

        ArgumentCaptor<String> texts = ArgumentCaptor.forClass(String.class);
        verify(statusMethods, times(20)).postStatus(texts.capture(), anyList(), any(), any(), anyBoolean(), any(), any());
        assertThat(texts.getAllValues()).allSatisfy(text -> {
            String[] paragraphs = text.split("\n\n");
            assertThat(greetings).contains(paragraphs[0]);
            assertThat(announcements).contains(paragraphs[1]);
        });
    }

    @Test
    void postStatusShouldNotPostStatusIfUploadFails() throws BigBoneRequestException {
        when(mediaRequest.execute()).thenThrow(new BigBoneRequestException("upload failed"));

        assertThatThrownBy(() -> repository.postStatus(yaml, image))
                .isInstanceOf(BigBoneRequestException.class)
                .hasMessage("upload failed");
        verifyNoInteractions(statusMethods);
    }

    @Test
    void postStatusShouldPropagateExceptionIfPostingFails() throws BigBoneRequestException {
        when(statusRequest.execute()).thenThrow(new BigBoneRequestException("post failed"));

        assertThatThrownBy(() -> repository.postStatus(yaml, image))
                .isInstanceOf(BigBoneRequestException.class)
                .hasMessage("post failed");
    }
}

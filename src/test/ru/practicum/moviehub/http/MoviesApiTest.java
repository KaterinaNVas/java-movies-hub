package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp =
                sendGetRequest("/movies");

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        assertJsonContentType(resp);

        JsonArray movies = JsonParser
                .parseString(resp.body())
                .getAsJsonArray();

        assertEquals(
                0,
                movies.size(),
                "При пустом хранилище должен вернуться пустой JSON-массив"
        );
    }

    @Test
    void createMovie_whenDataValid_returnsCreatedMovie() throws Exception {
        String requestBody = """
            {
              "title": "Матрица",
              "year": 1999
            }
            """;

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertCreatedMovie(
                resp,
                "Матрица",
                1999
        );
    }

    @Test
    void createMovie_whenTitleEmpty_returnsValidationError() throws Exception {
        String requestBody = """
            {
              "title": "",
              "year": 1999
            }
            """;

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertValidationError(
                resp,
                "название не должно быть пустым"
        );
    }

    @Test
    void createMovie_whenTitleTooLong_returnsValidationError() throws Exception {
        String tooLongTitle = "t".repeat(101);

        String requestBody = """
            {
              "title": "%s",
              "year": 1999
            }
            """.formatted(tooLongTitle);

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertValidationError(
                resp,
                "название не должно быть длиннее 100 символов"
        );
    }

    @Test
    void createMovie_whenYearTooSmall_returnsValidationError() throws Exception {
        String requestBody = """
            {
              "title": "Матрица",
              "year": 1887
            }
            """;

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertValidationError(
                resp,
                "Год не должен быть ранее 1888"
        );
    }

    @Test
    void createMovie_whenYearTooLarge_returnsValidationError() throws Exception {
        int year = LocalDate.now().getYear() + 2;

        String requestBody = """
            {
              "title": "Матрица",
              "year": %d
            }
            """.formatted(year);

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertValidationError(
                resp,
                "Год не должен быть позднее текущего более чем на один год"
        );
    }


    @Test
    void createMovie_whenContentTypeInvalid_returnsUnsupportedMediaType() throws Exception {
        String requestBody = """
            {
              "title": "Матрица",
              "year": 1995
            }
            """;

        HttpResponse<String> resp =
                sendPostRequest(requestBody, "text/plain");

        assertEquals(
                415,
                resp.statusCode(),
                "POST /movies с неправильным Content-Type должен вернуть 415"
        );

        assertJsonContentType(resp);

        JsonObject responseJson = JsonParser
                .parseString(resp.body())
                .getAsJsonObject();

        assertTrue(
                responseJson.has("error"),
                "Ответ должен содержать поле error"
        );

        assertTrue(
                !responseJson.get("error").getAsString().isBlank(),
                "Поле error не должно быть пустым"
        );
    }

    @Test
    void createMovie_whenJsonInvalid_returnsBadRequest() throws Exception{
        String requestBody = """
            {
              "title": "Матрица",
              "year":
            }
            """;

        HttpResponse<String> resp =
                sendPostRequest(requestBody);

        assertEquals(
                400,
                resp.statusCode(),
                "POST /movies с некорректным JSON должен вернуть 400 Bad Request"
        );

        assertJsonContentType(resp);

        JsonObject responseJson = JsonParser
                .parseString(resp.body())
                .getAsJsonObject();

        assertTrue(
                responseJson.has("error"),
                "Ответ должен содержать поле error"
        );

        assertTrue(
                !responseJson.get("error")
                        .getAsString()
                        .isBlank(),
                "Поле error не должно быть пустым"
        );
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        String requestBody = """
                {
                  "title": "Матрица",
                  "year": 1999
                }
                """;

        HttpResponse<String> createResponse =
                sendPostRequest(requestBody);

        int movieId = assertCreatedMovie(
                createResponse,
                "Матрица",
                1999
        );

        HttpResponse<String> getResponse =
                sendGetRequest("/movies/" + movieId);

        assertEquals(
                200,
                getResponse.statusCode(),
                "GET /movies/{id} существующего фильма должен вернуть 200"
        );

        assertJsonContentType(getResponse);

        JsonObject movieJson = JsonParser
                .parseString(getResponse.body())
                .getAsJsonObject();

        assertTrue(
                movieJson.has("id"),
                "Ответ должен содержать поле id"
        );

        assertEquals(
                movieId,
                movieJson.get("id").getAsInt(),
                "ID найденного фильма должен совпадать"
        );

        assertTrue(
                movieJson.has("title"),
                "Ответ должен содержать поле title"
        );

        assertEquals(
                "Матрица",
                movieJson.get("title").getAsString(),
                "Название найденного фильма должно совпадать"
        );

        assertTrue(
                movieJson.has("year"),
                "Ответ должен содержать поле year"
        );

        assertEquals(
                1999,
                movieJson.get("year").getAsInt(),
                "Год найденного фильма должен совпадать"
        );
    }

        private HttpResponse<String> sendPostRequest(
            String requestBody
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody,
                        StandardCharsets.UTF_8
                ))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> sendPostRequest(
            String requestBody,
            String contentType
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody,
                        StandardCharsets.UTF_8
                ))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private void assertJsonContentType(HttpResponse<String> response) {
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentType,
                "Content-Type должен указывать JSON и кодировку UTF-8"
        );
    }


    private void assertValidationError(
            HttpResponse<String> response,
            String... expectedDetails
    ) {
        assertEquals(
                422,
                response.statusCode(),
                "При ошибке валидации сервер должен вернуть 422"
        );

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser
                .parseString(response.body())
                .getAsJsonObject();

        assertTrue(
                responseJson.has("error"),
                "Ответ должен содержать поле error"
        );

        assertEquals(
                "Ошибка валидации",
                responseJson.get("error").getAsString(),
                "Поле error должно описывать ошибку валидации"
        );

        assertTrue(
                responseJson.has("details"),
                "Ответ должен содержать поле details"
        );

        assertTrue(
                responseJson.get("details").isJsonArray(),
                "Поле details должно быть JSON-массивом"
        );

        JsonArray actualDetails =
                responseJson.getAsJsonArray("details");

        assertEquals(
                expectedDetails.length,
                actualDetails.size(),
                "Количество ошибок валидации должно совпадать"
        );

        for (int i = 0; i < expectedDetails.length; i++) {
            assertEquals(
                    expectedDetails[i],
                    actualDetails.get(i).getAsString(),
                    "Текст ошибки валидации должен совпадать"
            );
        }
    }

    private int assertCreatedMovie(
            HttpResponse<String> response,
            String expectedTitle,
            int expectedYear
    ) {
        assertEquals(
                201,
                response.statusCode(),
                "POST /movies должен вернуть 201 Created"
        );

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser
                .parseString(response.body())
                .getAsJsonObject();

        assertTrue(
                responseJson.has("id"),
                "Созданному фильму должен быть присвоен ID"
        );

        int id = responseJson.get("id").getAsInt();

        assertTrue(
                id > 0,
                "ID фильма должен быть положительным числом"
        );

        assertTrue(
                responseJson.has("title"),
                "Ответ должен содержать поле title"
        );

        assertEquals(
                expectedTitle,
                responseJson.get("title").getAsString(),
                "Название созданного фильма должно совпадать"
        );

        assertTrue(
                responseJson.has("year"),
                "Ответ должен содержать поле year"
        );

        assertEquals(
                expectedYear,
                responseJson.get("year").getAsInt(),
                "Год созданного фильма должен совпадать"
        );

        return id;
    }

    private HttpResponse<String> sendGetRequest(
            String path
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );
    }
}
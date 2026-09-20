package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = sendGetRequest("/movies");

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        assertJsonContentType(resp);

        JsonArray movies = JsonParser.parseString(resp.body()).getAsJsonArray();

        assertEquals(0, movies.size(), "При пустом хранилище должен вернуться пустой JSON-массив");
    }

    @Test
    void createMovie_whenDataValid_returnsCreatedMovie() throws Exception {
        String requestBody = createMovieJson("Матрица", 1999);

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertCreatedMovie(resp, "Матрица", 1999);
    }

    @Test
    void createMovie_whenTitleEmpty_returnsValidationError() throws Exception {
        String requestBody = createMovieJson("", 1999);

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertValidationError(resp, "название не должно быть пустым");
    }

    @Test
    void createMovie_whenTitleTooLong_returnsValidationError() throws Exception {
        String tooLongTitle = "t".repeat(101);

        String requestBody = createMovieJson(tooLongTitle, 1999);

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertValidationError(resp, "название не должно быть длиннее 100 символов");
    }

    @Test
    void createMovie_whenYearTooSmall_returnsValidationError() throws Exception {
        String requestBody = createMovieJson("Матрица", 1887);

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertValidationError(resp, "Год не должен быть ранее 1888");
    }

    @Test
    void createMovie_whenYearTooLarge_returnsValidationError() throws Exception {
        int year = LocalDate.now().getYear() + 2;

        String requestBody = createMovieJson("Матрица", year);

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertValidationError(resp, "Год не должен быть позднее текущего более чем на один год");
    }


    @Test
    void createMovie_whenContentTypeInvalid_returnsUnsupportedMediaType() throws Exception {
        String requestBody = createMovieJson("Матрица", 1995);

        HttpResponse<String> resp = sendPostRequest(requestBody, "text/plain");

        assertEquals(415, resp.statusCode(), "POST /movies с неправильным Content-Type должен вернуть 415");

        assertJsonContentType(resp);

        JsonObject responseJson = JsonParser.parseString(resp.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertFalse(responseJson.get("error").getAsString().isBlank(), "Поле error не должно быть пустым");
    }

    @Test
    void createMovie_whenJsonInvalid_returnsBadRequest() throws Exception {
        String requestBody = "{\"title\":\"Матрица\",\"year\":}";

        HttpResponse<String> resp = sendPostRequest(requestBody);

        assertEquals(400, resp.statusCode(), "POST /movies с некорректным JSON должен вернуть 400 Bad Request");

        assertJsonContentType(resp);

        JsonObject responseJson = JsonParser.parseString(resp.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertFalse(responseJson.get("error").getAsString().isBlank(), "Поле error не должно быть пустым");
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        String requestBody = createMovieJson("Матрица", 1999);

        HttpResponse<String> createResponse = sendPostRequest(requestBody);

        int movieId = assertCreatedMovie(createResponse, "Матрица", 1999);

        HttpResponse<String> getResponse = sendGetRequest("/movies/" + movieId);

        assertEquals(200, getResponse.statusCode(), "GET /movies/{id} существующего фильма должен вернуть 200");

        assertJsonContentType(getResponse);

        JsonObject movieJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();

        assertTrue(movieJson.has("id"), "Ответ должен содержать поле id");

        assertEquals(movieId, movieJson.get("id").getAsInt(), "ID найденного фильма должен совпадать");

        assertTrue(movieJson.has("title"), "Ответ должен содержать поле title");

        assertEquals("Матрица", movieJson.get("title").getAsString(), "Название найденного фильма должно совпадать");

        assertTrue(movieJson.has("year"), "Ответ должен содержать поле year");

        assertEquals(1999, movieJson.get("year").getAsInt(), "Год найденного фильма должен совпадать");
    }

    @Test
    void getMovieById_whenMovieNotFound_returnsNotFound() throws Exception {
        HttpResponse<String> response = sendGetRequest("/movies/999");

        assertEquals(404, response.statusCode(), "GET /movies/{id} для несуществующего фильма должен вернуть 404");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Фильм не найден", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    @Test
    void getMovieById_whenIdNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> response = sendGetRequest("/movies/abc");

        assertEquals(400, response.statusCode(), "GET /movies/{id} с нечисловым ID должен вернуть 400");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Некорректный ID", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    @Test
    void getMovies_whenMoviesExist_returnsMovies() throws Exception {
        String requestBody = createMovieJson("Матрица", 1999);

        HttpResponse<String> createResponse = sendPostRequest(requestBody);

        int movieId = assertCreatedMovie(createResponse, "Матрица", 1999);

        HttpResponse<String> getResponse = sendGetRequest("/movies");

        assertEquals(200, getResponse.statusCode(), "GET /movies должен вернуть 200");

        assertJsonContentType(getResponse);

        JsonArray movies = JsonParser.parseString(getResponse.body()).getAsJsonArray();

        assertEquals(1, movies.size(), "В списке должен находиться один фильм");

        JsonObject movieJson = movies.get(0).getAsJsonObject();

        assertEquals(movieId, movieJson.get("id").getAsInt(), "ID фильма должен совпадать");

        assertEquals("Матрица", movieJson.get("title").getAsString(), "Название фильма должно совпадать");

        assertEquals(1999, movieJson.get("year").getAsInt(), "Год фильма должен совпадать");
    }

    @Test
    void deleteMovie_whenMovieExists_returnsNoContent() throws Exception {
        String requestBody = createMovieJson("Матрица", 1999);

        HttpResponse<String> createResponse = sendPostRequest(requestBody);

        int movieId = assertCreatedMovie(createResponse, "Матрица", 1999);

        HttpResponse<String> deleteResponse = sendDeleteRequest("/movies/" + movieId);

        assertEquals(204, deleteResponse.statusCode(), "DELETE /movies/{id} существующего фильма должен вернуть 204");

        assertEquals("", deleteResponse.body(), "Ответ 204 No Content не должен содержать тело");

        HttpResponse<String> getResponse = sendGetRequest("/movies/" + movieId);

        assertEquals(404, getResponse.statusCode(), "После удаления фильм не должен находиться в хранилище");
    }

    @Test
    void deleteMovie_whenMovieNotFound_returnsNotFound() throws Exception {
        HttpResponse<String> response = sendDeleteRequest("/movies/999");

        assertEquals(404, response.statusCode(), "DELETE /movies/{id} для несуществующего фильма должен вернуть 404");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Фильм не найден", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    @Test
    void deleteMovie_whenIdNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> response = sendDeleteRequest("/movies/abc");

        assertEquals(400, response.statusCode(), "DELETE /movies/{id} с нечисловым ID должен вернуть 400");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Некорректный ID", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    @Test
    void getMoviesByYear_whenMoviesExist_returnsMatchingMovies() throws Exception {
        String matrixJson = createMovieJson("Матрица", 1999);
        String inceptionJson = createMovieJson("Начало", 2010);

        sendPostRequest(matrixJson);
        sendPostRequest(inceptionJson);

        HttpResponse<String> response = sendGetRequest("/movies?year=1999");

        assertEquals(200, response.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        assertJsonContentType(response);

        JsonArray movies = JsonParser.parseString(response.body()).getAsJsonArray();

        assertEquals(1, movies.size(), "Должен вернуться только один фильм указанного года");

        JsonObject movieJson = movies.get(0).getAsJsonObject();

        assertEquals("Матрица", movieJson.get("title").getAsString(), "Должен вернуться фильм указанного года");

        assertEquals(1999, movieJson.get("year").getAsInt(), "Год фильма должен совпадать с параметром запроса");
    }

    @Test
    void getMoviesByYear_whenNoMoviesFound_returnsEmptyArray() throws Exception {
        String requestBody = createMovieJson("Матрица", 1999);

        sendPostRequest(requestBody);

        HttpResponse<String> response = sendGetRequest("/movies?year=2010");

        assertEquals(200, response.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        assertJsonContentType(response);

        JsonArray movies = JsonParser.parseString(response.body()).getAsJsonArray();

        assertEquals(0, movies.size(), "Если фильмов указанного года нет, должен вернуться пустой массив");
    }

    @Test
    void getMoviesByYear_whenYearNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> response = sendGetRequest("/movies?year=abc");

        assertEquals(400, response.statusCode(), "GET /movies с нечисловым параметром year должен вернуть 400");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Некорректный параметр запроса — 'year'", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    @Test
    void getMovies_whenQueryParameterInvalid_returnsBadRequest() throws Exception {
        HttpResponse<String> response = sendGetRequest("/movies?foo=1999");

        assertEquals(400, response.statusCode(), "GET /movies с неподдерживаемым параметром должен вернуть 400");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Некорректный параметр запроса — 'year'", responseJson.get("error").getAsString(), "Сообщение должно указывать на корректный параметр year");
    }

    @Test
    void movies_whenMethodUnsupported_returnsMethodNotAllowed() throws Exception {
        HttpResponse<String> response = sendPutRequest("/movies");

        assertEquals(405, response.statusCode(), "Неподдерживаемый HTTP-метод должен вернуть 405");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Метод не поддерживается", responseJson.get("error").getAsString(), "Сообщение об ошибке должно совпадать");
    }

    private HttpResponse<String> sendPutRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + path)).timeout(Duration.ofSeconds(2)).PUT(HttpRequest.BodyPublishers.noBody()).build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDeleteRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + path)).timeout(Duration.ofSeconds(2)).DELETE().build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }


    private HttpResponse<String> sendPostRequest(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).timeout(Duration.ofSeconds(2)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPostRequest(String requestBody, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).timeout(Duration.ofSeconds(2)).header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertJsonContentType(HttpResponse<String> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentType, "Content-Type должен указывать JSON и кодировку UTF-8");
    }


    private void assertValidationError(HttpResponse<String> response, String... expectedDetails) {
        assertEquals(422, response.statusCode(), "При ошибке валидации сервер должен вернуть 422");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("error"), "Ответ должен содержать поле error");

        assertEquals("Ошибка валидации", responseJson.get("error").getAsString(), "Поле error должно описывать ошибку валидации");

        assertTrue(responseJson.has("details"), "Ответ должен содержать поле details");

        assertTrue(responseJson.get("details").isJsonArray(), "Поле details должно быть JSON-массивом");

        JsonArray actualDetails = responseJson.getAsJsonArray("details");

        assertEquals(expectedDetails.length, actualDetails.size(), "Количество ошибок валидации должно совпадать");

        for (int i = 0; i < expectedDetails.length; i++) {
            assertEquals(expectedDetails[i], actualDetails.get(i).getAsString(), "Текст ошибки валидации должен совпадать");
        }
    }

    private int assertCreatedMovie(HttpResponse<String> response, String expectedTitle, int expectedYear) {
        assertEquals(201, response.statusCode(), "POST /movies должен вернуть 201 Created");

        assertJsonContentType(response);

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        assertTrue(responseJson.has("id"), "Созданному фильму должен быть присвоен ID");

        int id = responseJson.get("id").getAsInt();

        assertTrue(id > 0, "ID фильма должен быть положительным числом");

        assertTrue(responseJson.has("title"), "Ответ должен содержать поле title");

        assertEquals(expectedTitle, responseJson.get("title").getAsString(), "Название созданного фильма должно совпадать");

        assertTrue(responseJson.has("year"), "Ответ должен содержать поле year");

        assertEquals(expectedYear, responseJson.get("year").getAsInt(), "Год созданного фильма должен совпадать");

        return id;
    }

    private HttpResponse<String> sendGetRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + path)).timeout(Duration.ofSeconds(2)).GET().build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String createMovieJson(
            String title,
            int year
    ) {
        return String.format(
                "{\"title\":\"%s\",\"year\":%d}",
                title,
                year
        );
    }
}

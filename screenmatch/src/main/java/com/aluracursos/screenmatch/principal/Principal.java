package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=" + System.getenv("API_KEY_OMDB"); //3e7932e3
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    \n
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar en series guardadas
                    5 - Top 5 mejores series
                    6 - Buscar series por categoría
                    7 - Buscar series por numero de temporadas
                    8 - Buscar series a partir de una evaluacion
                    9 - Filtrar series por máximo de temporadas y evaluación mínima
                    10 - Buscar episodios por título
                    11 - Top 5 episodios por serie
                                  
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    buscarTop5Series();
                    break;
                case 6:
                    buscarSeriesPorCategoria();
                    break;
                case 7:
                    buscarSeriesPorNumeroDeTemporadas();
                    break;
                case 8:
                    buscarSeriesAPartirDeUnaEvaluacion();
                    break;
                case 9:
                    buscarSeriesPorMaximoDeTemporadasYMinimoDeEvaluacion();
                    break;
                case 10:
                    buscarEpisodiosPorTitulo();
                    break;
                case 11:
                    buscarTop5Episodios();
                    break;
                case 0:
                    System.out.println("\nCerrando la aplicación...\n");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }

    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }

    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("\n¿De qué serie quieres ver los episodios?");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();

            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);


            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }
    }

    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        serie = repositorio.save(serie);
        //datosSeries.add(datos);
        System.out.println(datos);

    }

    private void mostrarSeriesBuscadas() {

        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        serieBuscada = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("La serie es: " + serieBuscada.get());
        } else {
            System.out.println("Serie no encontrada :[ ");
        }

    }

    private void buscarTop5Series() {
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s ->
                System.out.println("Serie: " + s.getTitulo() + " - Evaluación: " + s.getEvaluacion()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("¿Qué género/categoría quieres buscar? ");
        var genero = teclado.nextLine().toLowerCase();
        var categoria = Categoria.fromEspanol(genero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("\nLas series de la categoría *** " + genero + " ***");
        seriesPorCategoria.forEach(s ->
                System.out.println(s.getTitulo()));
    }

    private void buscarSeriesPorNumeroDeTemporadas() {
        System.out.println("¿De cuántas temporadas quieres buscar series?");
        var numeroDeTemporadas = teclado.nextInt();
        List<Serie> seriesPorNumeroDeTemporadas = repositorio.findByTotalTemporadas(numeroDeTemporadas);
        System.out.println("\nLas series con " + numeroDeTemporadas + " temporadas son: ");
        seriesPorNumeroDeTemporadas.forEach(s -> System.out.println(s.getTitulo()));
    }

    private void buscarSeriesAPartirDeUnaEvaluacion() {
        System.out.println("¿A partir de que evaluacion quieres buscar series?");
        var evaluacionGreaterThanEqual = teclado.nextDouble();
        List<Serie> seriesAPartirDeUnaEvaluacion = repositorio.findByEvaluacionGreaterThanEqual(evaluacionGreaterThanEqual);
        System.out.println("\nLas series de " + evaluacionGreaterThanEqual + " o mayor evaluación son: ");
        seriesAPartirDeUnaEvaluacion.forEach(s -> System.out.println(
                s.getTitulo() + " - Evaluación: " + s.getEvaluacion()
        ));
    }

    private void buscarSeriesPorMaximoDeTemporadasYMinimoDeEvaluacion() {
        System.out.println("¿Máximo de cuantas temporadas?");
        var maximoTemporadas = teclado.nextInt();
        System.out.println("¿Evaluación mínima?");
        var evaluacionMinima = teclado.nextDouble();
        List<Serie> seriesFiltradas = repositorio.seriesPorTemporadaYEvaluacion(maximoTemporadas, evaluacionMinima);
        System.out.println("\n *** Series filtradas *** ");
        seriesFiltradas.forEach(s ->
                System.out.println(
                        s.getTitulo() + " - Temporadas: " + s.getTotalTemporadas() + " - Evaluación: " + s.getEvaluacion()
                ));
    }

    private void buscarEpisodiosPorTitulo() {
        System.out.println("Escribe el nombre del episodio que deseas buscar");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Serie: %s - Temporada: %s - Episodio: %s - Evaluación: %s ",
                        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()
                ));
    }

    private void buscarTop5Episodios() {
        buscarSeriesPorTitulo();
        if(serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            List<Episodio> top5Episodios = repositorio.top5Episodios(serie);
            top5Episodios.forEach(e ->
                    System.out.printf("\nEpisodio: %s - Temporada: %s - Evaluación: %s ",
                            e.getTitulo(), e.getTemporada(), e.getEvaluacion()
                    ));
        }
    }


}


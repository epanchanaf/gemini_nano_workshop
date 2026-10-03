# Taller de Gemini Nano 🚀

Aplicación Android de demostración para explorar capacidades de IA generativa en el dispositivo con **Gemini Nano** y las APIs GenAI de **ML Kit**. La interfaz está en español y permite probar tareas de texto e imagen sin enviar el contenido a un servicio de generación en la nube.

> El modelo o las funciones de ML Kit pueden requerir una descarga inicial desde el dispositivo compatible. La app muestra el estado de disponibilidad y el progreso de descarga antes de habilitar cada función.

## Funcionalidades

- **Resumen:** convierte un artículo o conversación en un resumen de tres viñetas.
- **Corrección:** detecta y corrige errores en mensajes cortos.
- **Reescritura:** transforma un texto para reformularlo, acortarlo, ampliarlo o darle un tono amigable o profesional.
- **Descripción de imágenes:** selecciona una imagen del dispositivo y genera una descripción breve y objetiva.
- **Chat:** genera respuestas de texto por instrucciones y las muestra de forma progresiva.
- **Estado del modelo:** consulta la disponibilidad de AI Core y de las funciones GenAI necesarias antes de usarlas.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3
- Navigation Compose para la navegación entre pantallas
- Hilt para inyección de dependencias
- Kotlin Coroutines y Flow para operaciones asíncronas y respuestas en streaming
- ML Kit GenAI: Summarization, Proofreading, Rewriting, Image Description y Prompt/Generation
- Gemini Nano a través de AI Core

## Arquitectura de la app

La aplicación emplea una arquitectura **MVVM** orientada a estado. Cada funcionalidad mantiene su propia pantalla, `ViewModel`, estado de UI y repositorio; la navegación conecta esas pantallas desde un único `NavHost`.

```text
Usuario
   │
   ▼
Pantallas Compose ──► ViewModel ──► Repositorio ──► ML Kit GenAI ──► AICore ──► Gemini Nano
   ▲                     │                │                              │
   └──── StateFlow ◄─────┴──── resultado, errores y progreso ─────────────┘
```

| Capa | Responsabilidad | Ubicación |
| --- | --- | --- |
| Presentación | Pantallas Compose, componentes reutilizables, tema y navegación. | `ui/` |
| Estado | Recibe las acciones del usuario, expone `StateFlow`, ejecuta corrutinas y adapta el progreso o los errores para la interfaz. | `ui/home/**/**ViewModel.kt` |
| Datos/IA | Crea los clientes de ML Kit, comprueba disponibilidad, descarga componentes y solicita la inferencia. | `data/genai/repository/` |
| Plataforma | `AICore` ejecuta y administra el modelo Gemini Nano disponible en el dispositivo. | Servicio de Android |

**Flujo de una capacidad:** la pantalla envía una acción al `ViewModel`; este consulta el estado de la función en su repositorio. Si el componente está disponible, ejecuta la inferencia; si es descargable, observa el progreso antes de continuar; si no es compatible, entrega un mensaje de error a la UI. Los repositorios se cierran al finalizar el ciclo de vida de su `ViewModel` para liberar los clientes de IA.

Hilt proporciona los repositorios a los `ViewModel`, y `Navigation Compose` separa las rutas de resumen, corrección, reescritura, descripción de imágenes y chat. El chat usa una respuesta en *streaming*, por lo que la UI añade fragmentos de texto a medida que el modelo los genera.

Los repositorios reciben una `GenAiClientFactory` por constructor. Hilt la enlaza con `MlKitGenAiClientFactory` mediante `GenAiModule`; esta implementación concentra las llamadas a `getClient()` y las opciones de ML Kit. La fábrica no almacena clientes: cada repositorio conserva y cierra los que solicita. El repositorio de estado crea clientes temporales por consulta y los cierra al terminar. En pruebas se puede pasar otra implementación de la fábrica para sustituir los clientes sin inicializar AICore.

El contrato de errores de datos es `AiException`: distingue entrada inválida, modelo no disponible, fallo de descarga, resultado vacío y fallos de consulta o generación. Los repositorios validan las entradas y las respuestas finales, y conservan la excepción original como causa para diagnóstico. En streaming, los fragmentos vacíos son válidos; terminar sin texto útil produce `EmptyResult`. Las cancelaciones se propagan como `CancellationException`. Cada `ViewModel` usa el mismo mapeador de mensajes de UI, sin mostrar los mensajes internos del SDK. El método `prepare()` de los repositorios consulta disponibilidad y descarga cuando es necesario; si ya hay una descarga en curso, permite informar al usuario y reintentar.

Cada operación de usuario expone un `Flow<AiOperationState<T>>`. El flujo ordena la validación, la preparación, el progreso de descarga, la generación parcial, el resultado final y los errores. Las pantallas solo reducen esos estados a su estado visual; no conocen el orden de llamadas ni las excepciones de ML Kit.

La demo muestra y aplica límites de entrada antes de la inferencia: resumen acepta al menos 400 caracteres y hasta 4 000 tokens estimados, mientras que corrección, reescritura y chat se limitan a 1 024 tokens estimados para mantener los ejemplos breves. El chat además consulta el conteo y límite reales del modelo antes de generar. La descripción de imágenes ajusta las selecciones a un máximo de 2 048 píxeles por lado y 4 megapíxeles.

## Estructura del proyecto

```text
gemini-nano-workshop-main/
├── app/                              # Aplicación Android principal
│   └── src/main/
│       ├── java/com/sdkecuador/gemininano/
│       │   ├── data/genai/           # Clientes de ML Kit, descargas y estados de AI Core
│       │   └── ui/                   # Pantallas Compose, ViewModels, navegación y tema
│       └── res/                      # Recursos, iconos y tipografías
├── gradle/                           # Version catalog y Gradle Wrapper
├── build.gradle.kts                  # Configuración de compilación raíz
└── settings.gradle.kts               # Módulos y repositorios del proyecto
```

El código ejecutable está en el módulo `app/`.

## Gemini Nano y AICore

[Gemini Nano](https://developer.android.com/ai/gemini-nano) es un modelo fundacional de Google optimizado para ejecutarse en el dispositivo. En Android se accede a él a través de **AICore**, el servicio del sistema que administra el modelo, sus actualizaciones, funciones de seguridad y el uso del hardware local.

Esta app no empaqueta un modelo propio ni se conecta directamente al modelo: utiliza las [APIs GenAI de ML Kit](https://developers.google.com/ml-kit/genai), una capa de alto nivel sobre AICore. De esta manera puede implementar tareas ya preparadas —resumen, corrección, reescritura y descripción de imágenes— además de solicitudes personalizadas mediante la API Prompt que utiliza el chat.

Una vez descargado el componente correspondiente, las entradas, la inferencia y las respuestas se procesan localmente. Esto reduce la dependencia de conectividad y evita enviar el contenido a un servidor de generación. AICore puede compartir el modelo entre aplicaciones compatibles, lo que evita copias independientes del modelo y ayuda a conservar almacenamiento.

La disponibilidad no es universal: cambia según el modelo Gemini Nano, el fabricante, el dispositivo y la API utilizada. La app consulta ese estado antes de cada acción, pero es recomendable comprobar la [compatibilidad actual de dispositivos](https://developers.google.com/ml-kit/genai#device-support) antes de probarla. Las APIs GenAI incluidas en este proyecto se distribuyen actualmente en beta; Google también aplica cuotas por aplicación y permite inferencia solo cuando la app está en primer plano.

## Requisitos

- Android Studio con soporte para proyectos Kotlin/Compose y JDK 17.
- Un dispositivo Android físico compatible con Gemini Nano y AI Core. Consulta la [lista de dispositivos compatibles](https://developers.google.com/ml-kit/genai).
- Conexión a internet durante la configuración inicial y cuando el dispositivo deba descargar los modelos.
- Android 14 (API 34) o superior; la app compila contra API 36.

## Ejecutar la aplicación

1. Abre esta carpeta, `gemini-nano-workshop-main`, en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Conecta un dispositivo compatible, selecciónalo como destino y ejecuta la app.
4. En la pantalla principal, revisa el estado de cada capacidad y descarga el componente requerido si la app lo solicita.

También puedes generar un APK de depuración desde la terminal:

```bash
./gradlew assembleDebug
```

El APK resultante queda en `app/build/outputs/apk/debug/`.

## Notas de uso

- Las APIs de resumen, corrección y reescritura están configuradas actualmente para procesar contenido en inglés, aunque los textos de la interfaz estén en español.
- La disponibilidad de Gemini Nano depende del hardware, la versión de Android y de AI Core instalados en el dispositivo.
- Las funciones de IA generativa se ejecutan localmente una vez que el componente necesario está disponible en el equipo.

## Autor

**Edgardo Panchana**

- 💼 [LinkedIn](https://www.linkedin.com/in/edgardopanchana/)
- 📸 [Instagram](https://www.instagram.com/edgardopanchana/)

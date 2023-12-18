import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.IOException
import java.io.BufferedReader
import java.io.FileReader


fun main() {
    val mainLane = "C:\\Users\\Admin\\Testing"
    //в этой переменной нужно прописать путь к родительскому каталогу
    //В моем случае родительский каталог - папка "Testing", в которой находятся все остальные папки и текстовые файлы

    val treeSon = Tree(mainLane)
    treeSon.mainFileFinder()
}

class Tree(_parentFilePath: String){

    private val parentFilePath = _parentFilePath//путь к родительскому каталогу
    private val filesCollection = mutableListOf<String>()//коллекция со всеми файлами
    private var addictionsCollection = mutableListOf<String>()//коллекция со всеми зависимостями txt фалов(необработанная)

    //По соображениям приватности перенес вызов основных функций в этот метод, что бы не вызывать из main'а
    fun mainFileFinder(){
        getParentsItems(parentFilePath)
        writerToTXT()
        getAddictions()
        writerSortedToTXT(sortAddictions())
    }

    //Получаем элементы родительского каталога и сохраняем в коллекцию
    //Метод вызывается первым, что бы в него не писались файлы, которые будут добавляться позже
    fun getParentsItems(currentFilePath: String)
    {
        val currentDirectory = File(currentFilePath)
        for(item in currentDirectory.list()){
            filesCollection.add("$currentFilePath"+"\\"+"$item")
        }
        for(item in currentDirectory.list()){
            if (File("$currentFilePath"+"\\"+"$item").list()!=null){
                getParentsItems("$currentFilePath"+"\\"+"$item")
            }
        }
    }

    //стандартный метод для инициализации с записью нового файла со всеми объектами родительского каталога
    fun writerToTXT(){

        // Путь к файлу для записи
        val filePath = "$parentFilePath"+"\\"+"AllFilesAndFolders.txt"

        // Запись элементов списка в файл
        try {
            val file = File(filePath)
            val writer = BufferedWriter(FileWriter(file))

            for (item in filesCollection) {
                writer.write(item)
                writer.newLine() // Добавляем новую строку для каждого элемента
            }
            writer.close()
            println("Элементы списка успешно записаны в файл: $filePath")

        } catch (e: IOException) {
            println("Произошла ошибка при записи в файл.")
            e.printStackTrace()
        }
    }
    //Метод, который находит все зависимости в заданном пути каталога
    //После чего сохраняет зависимые(под) и зависимые(над) в отдельную коллекцию requireList
    //На момент написания данного метода еще не открыл mutableMap, поэтому добавлял друг за другом
    //Из-за чего в последствии появились проблемы
    fun ReaderTxt(filePath: String): MutableList<String>{

        val requireList = mutableListOf<String>()
        val findedWord = "require"

        try {
            val file = File(filePath)
            val reader = BufferedReader(FileReader(file))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (findedWord in line!!) {
                    val fileUnder = filePath
                    val fileUpper = line!!.removePrefix("$findedWord ‘").dropLast(1)
                    requireList.add(fileUnder)
                    requireList.add(fileUpper)
                }
                else{
                    continue
                }
            }
            return requireList
        } catch (e: IOException) {
            println("Произошла ошибка при чтении файла.")
            e.printStackTrace()
            return requireList
        }
    }

    //в данном методе мы выявляем все искомые зависимости и сохраняем их
    fun getAddictions(){
        for(item in filesCollection){
            if(".txt" in item){
                for(element in ReaderTxt(item)){
                    addictionsCollection.add(element)
                }
            }
            else{
                continue
            }
        }
    }

    /*Самый некрасивый и громозкий метод, который выявляет циклические зависимости(если они есть) и
    Формирует коллекцию, порядок в которой построен для всех не циклических зависимостей
    Циклические зависимости заносятся в отдельный файл

    Алгоритм следующий:
    1) На момент вхождения мы имеем mutableList<String> с идущими друг за другом зависимостями (под) и (над)
    2) Из коллекции формируется две mutableMap, для одной из которых значения key и value меняются местами, так как для
    нас это пока две разные зависимости, а мы хотим сделать их одной
    3) Находим дубликаты в этих коллекциях
    4) Фильтруем дубликаты и получаем уникальные зависимости
    5) Создаем файл CycleAddictions.txt в родительском каталоге, в который и переносим все циклические зависимости
    6) Удаляем циклы, после чего формируем сортированный список с расположенными в требуемом порядке файлами
    Для сортировки списка используем топологическую сортировку
    7) На выходе имеем сортированный список
     */
    fun sortAddictions(): MutableList<String> {

        val addictionsSortedCollection = mutableMapOf<String, MutableList<String>>()
        val swappedCollection = mutableMapOf<String, MutableList<String>>()
        val indexesForDelate = mutableListOf<Int>()
        val cycleAddictions = mutableListOf<String>()

        for (i in 0 until addictionsCollection.size step 2) {
            val key = addictionsCollection[i]
            val value = addictionsCollection[i + 1]

            // Если ключ уже есть в Map, добавляем значение в существующий список
            if (addictionsSortedCollection.containsKey(key)) {
                addictionsSortedCollection[key]!!.add(value)
            } else {
                // Если ключа нет в Map, создаем новый список и добавляем в Map
                addictionsSortedCollection[key] = mutableListOf(value)
            }
        }


        for (i in 0 until addictionsCollection.size step 2) {
            val key = addictionsCollection[i+1]
            val value = addictionsCollection[i]

            // Если ключ уже есть в Map, добавляем значение в существующий список
            if (swappedCollection.containsKey(key)) {
                swappedCollection[key]!!.add(value)
            } else {
                // Если ключа нет в Map, создаем новый список и добавляем в Map
                swappedCollection[key] = mutableListOf(value)
            }
        }

        val duplicatesCollection = addictionsSortedCollection.entries
            .filter { (key) -> swappedCollection.containsKey(key) }
            .flatMap { (key, values) ->
                values.filter { value -> swappedCollection[key]!!.contains(value) }
                    .map { value -> key to value }
            }

        //обрабатываем дубли, которые получаются при развороте коллекции

        for ((key, value) in duplicatesCollection) {
            val firstKey = key
            val firstValue = value
            for((key, value) in duplicatesCollection) {
                if ((key == firstKey) && (value == firstValue)) {
                    if((cycleAddictions.contains("Циклическая зависимость между $firstKey и $firstValue")!=true)&&(cycleAddictions.contains("Циклическая зависимость между $firstValue и $firstKey")!=true)){
                        cycleAddictions.add("Циклическая зависимость между $firstKey и $firstValue")
                        for(i in 0..addictionsCollection.size-2 step 2){

                            if((addictionsCollection[i]==key&&addictionsCollection[i+1]==value)||(addictionsCollection[i]==value&&addictionsCollection[i+1]==key)){
                                indexesForDelate.add(i)
                                indexesForDelate.add(i+1)
                            }
                        }
                    }
                    else{
                        continue
                    }
                }
            }
        }

        cycleAddictions.add("Данные зависимости исключены из списка для сортировки")


        if(cycleAddictions.size>1){
            try {
                val file = File("$parentFilePath"+"\\"+"CycleAddictions.txt")
                val writer = BufferedWriter(FileWriter(file))

                for (item in cycleAddictions) {
                    writer.write(item)
                    writer.newLine()
                }
                writer.close()
                println("Элементы списка успешно записаны в файл $parentFilePath\\CycleAddictions.txt")
            }   catch (e: IOException) {
                println("Произошла ошибка при записи в файл.")
                e.printStackTrace()
            }
        }

        //проходим по коллекции с конца и исключаем элементы с индексами для удаления
        for(i in addictionsCollection.size-1 downTo 0){
            for(item in indexesForDelate){
                if(i==item){
                    addictionsCollection.removeAt(item)
                    break
                }
            }
        }

        /* Просто забавная отсылочка, сперва хотел назначать файлам потенциалы, пока не прочел про
        топологическую сортировку
        for(i in 0..addictionsCollection.size-1){
            if(i%2==0){
                potencialArray[i]--
            }
            else{
                potencialArray[i]++
            }
        }
        */


        val dependencies = mutableMapOf<String, MutableList<String>>()

        for (i in 0 until addictionsCollection.size - 1 step 2) {
            val file = addictionsCollection[i]
            val dependentFile = addictionsCollection[i + 1]

            dependencies.computeIfAbsent(dependentFile) { mutableListOf() }.add(file)
        }

        val sortedFiles = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        fun dfs(file: String) {
            visited.add(file)
            dependencies[file]?.forEach { dependentFile ->
                if (dependentFile !in visited) {
                    dfs(dependentFile)
                }
            }
            sortedFiles.add(file)
        }

        dependencies.keys.forEach { file ->
            if (file !in visited) {
                dfs(file)
            }
        }

        // Результат
        sortedFiles.reverse()
        return(sortedFiles)
    }

    //Создание и запись значений сортированного списка в файл SortedFiles.txt родительского каталога
    fun writerSortedToTXT(listWithFiles: MutableList<String>){

        // Путь к файлу, в который пойдет запись
        val filePath = "$parentFilePath"+"\\"+"SortedFiles.txt"

        // Запись элементов списка в файл
        try {
            val file = File(filePath)
            val writer = BufferedWriter(FileWriter(file))
            for (item in listWithFiles) {
                writer.write(item)
                writer.newLine() // Добавляем новую строку для каждого элемента
            }

            writer.close()
            println("Элементы списка успешно записаны в файл: $filePath")
        } catch (e: IOException) {
            println("Произошла ошибка при записи в файл.")
            e.printStackTrace()
        }
    }

}
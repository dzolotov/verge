<?php
/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @Author Dmitrii Zolotov <zolotov@gathe.org>, Tikhon Tagunov <tagunov@gathe.org>, Yurii Senin <ShinoGir@gmail.com>
 */
require_once('config.php');
setlocale(LC_ALL, LOCALE);
bindtextdomain("messages", "./locale");
textdomain("messages");
?>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title><?php echo _("index title")?></title>
</head>
<body>
<h2><?php echo _("head title")?></h2>
<?php
  $t = file(PATH."/documents.properties");
  $data = array();
  foreach ($t as $s) {
    $row = array();
    $q = explode("=",$s);
    $id = $q[0];
    $fn = $q[1];
    $fn = substr($fn,strrpos($fn,"/")+1);
    $fn = substr($fn,0,strpos($fn,"."));
    $row["id"]=$id;
    $row["prefix"]=$fn;
    $descfile = file(PATH."/".$fn.".data");
    $description = substr($descfile[0],1);
    $row["description"]=$description;
    $data[]=$row;
  }
  
  echo "<table border='1'>";
  foreach ($data as $row) {
    echo "<tr>";
    echo "<td><a href='viewTemplate.php?id=".$row["id"]."'>".$row["id"]."</td>";
    echo "<td>".$row["description"]."</td>";
    echo "<td><a href='downloadTemplate.php?id=".$row["id"]."'>[скачать]</a></td>";
    echo "<td><a href='deleteTemplate.php?id=".$row["id"]."'>[X]</a></td>";
    echo "<td><a href='replaceTemplate.php?id=".$row["id"]."'>Заменить</a></td>";
    echo "<td><a href='listSamples.php?id=".$row["id"]."'>Список образцов</a></td>";
    echo "<td><a href='preview.php?id=".$row["id"]."'>Заполнить образцом</a></td>";
    echo "</tr>";
  }
  echo "</table>";
?>
<a href="createTemplate.php">Создать шаблон</a>
</body>
</html>
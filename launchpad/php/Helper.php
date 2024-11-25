<?php

class Helper
{
    private $config = null;

    static function getSortingIcon($column, $direction, $field): string
    {
        if ($column != $field) {
            return '<a href="?field=' . $column . '&direction=asc">';
        }
        if ($direction === 'asc') {
            return '<a href="?field=' . $column . '&direction=desc"> &#9650; ';
        } else {
            return '<a href="?field=' . $column . '&direction=asc">  &#9660; ';
        }
    }

    public function getStatistic(): array
    {
        return Helper::getRequest("http://" . getenv('JAVA_HOST') . ":" . getenv('JAVA_PORT') . "/api/v1/statistic/");
    }

    /**
     * @param string $url link;
     * @return array decoded JSON (array of objects);
     */
    static function getRequest(string $url): array
    {
        $curl = curl_init($url);
        curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
        $curlResult = curl_exec($curl);
        curl_close($curl);
        return json_decode($curlResult, true);
    }


    /**
     * @param int $available
     * @return string
     */
    static function printIcon(int $available): string
    {
        switch ($available) {
            case 1:
                return 'ok.png';
            case 0:
                return 'warning.png';
            case -1:
            default:
                return 'problem.png';
        }
    }

    /**
     * @param array $data
     * @param string $field
     * @param string $direction
     * @return array
     */
    static function sortData(array $data, string $field, string $direction): array
    {
        uasort($data, function ($a, $b) use ($field, $direction) {
            if ($a[$field] === $b[$field]) {
                if ($a['langCode'] === $b['langCode']) {
                    return 0;
                } else {
                    return strcmp($a['langCode'], $b['langCode']);
                }
            }
            if ($direction === 'asc') {
                return ($a[$field] < $b[$field]) ? -1 : 1;
            } else {
                return ($a[$field] > $b[$field]) ? -1 : 1;
            }
        });
        return $data;
    }

    /**
     * @param array $data field of which should be validated;
     * @return array
     */
    static function prepareFlagField(array $data): array
    {
        $pattern = 'd-M-Y';

        array_walk($data, function (&$value) use ($pattern) {
            $currentDate = new DateTime();
            $fileDate = DateTime::createFromFormat("d-M-Y :: H:i:s", $value["lastModified"], new DateTimeZone("UTC"));
            if ($currentDate->format($pattern) === $fileDate->format($pattern)) {
                $value["available"] = 1;
            } else if ($currentDate->sub(new DateInterval('P1D'))->format($pattern) === $fileDate->format($pattern)) {
                $value["available"] = 0;
            } else {
                $value["available"] = -1;
            }
        });

        return $data;
    }

    static function getFooter(): string
    {
        return '<footer id= "footer-id" class="bg-light text-center text-lg-start fixed-bottom">
                    <div class="text-center p-3" style="background-color: rgba(0, 0, 0, 0.2);">© 2018-2024 Copyright:
                        <a class="text-dark" href="https://github.com/saivrem">Denis Sheviakov</a>. All Rights Reserved
                    </div>
                </footer>';
    }

    public function languages(): array
    {

        $link = "http://" . getenv('JAVA_HOST') . ":" . getenv('JAVA_PORT') . "/icedata/api/v2/languages";
        $languages = Helper::getRequest($link);

        /*if (count($languages) === 0) {
            if ($this->init()) {
                $languages = $this->languages();
            }
        }*/
        return $languages;
    }

    public function init(): bool
    {

        $curl = curl_init();

        curl_setopt_array($curl, array(
            CURLOPT_URL => "http://" . getenv('JAVA_HOST') . ":" . getenv('JAVA_PORT') . "/api/v1/language/init",
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_ENCODING => '',
            CURLOPT_MAXREDIRS => 10,
            CURLOPT_TIMEOUT => 0,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
            CURLOPT_CUSTOMREQUEST => 'POST',
            CURLOPT_POSTFIELDS => "{
                                        \"userName\":\"" . getenv('ICECAT_USER') . "\" ,
                                        \"passWord\":\"" . getenv('ICECAT_PASSWORD') . "\"
                                   }",
            CURLOPT_HTTPHEADER => array(
                'Content-Type: application/json'
            )
        ));
        $response = curl_exec($curl);

        curl_close($curl);
        return $response === "Done";

    }
}
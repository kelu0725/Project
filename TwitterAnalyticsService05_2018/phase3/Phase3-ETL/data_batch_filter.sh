for i in {000..999}; do
        wget s3.amazonaws.com/cmucc-datasets/twitter/s18/part-r-00"$i".gz
        gunzip part-r-00"$i".gz
        cat part-r-00"$i" | python preprop_tsv.py > part-r-00"$i"_filtered.tsv
        rm part-r-00"$i"
done

cat part-r-* | sort -u > q4_data.tsv

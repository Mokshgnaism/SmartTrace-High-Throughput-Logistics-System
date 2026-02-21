 The pipeline is straightforward yet effective. Note: this is not a pipeline in the traditional sense.

*           PROJECT OVERVIEW
*   -> Generates pallet IDs and hashes them; pallets serve as the base containers for products.
*   -> Generates carton IDs; cartons are the primary packaging units inside pallets.
*   -> Generates unit IDs and hashes them; units represent individual products inside cartons.
*
*   KEY CHARACTERISTICS
*   -> Implements a fully streaming architecture with no blocking stages except when strictly necessary.
*
*   ARCHITECTURE
*   -> Two cooperating functions per stage:
*       -> One generates IDs and writes to a PipedOutputStream.
*       -> The other is a DB inserter with a connected PipedInputStream, consuming the stream.
*   -> Labels are streamed to PostgreSQL COPY as CSV with minimal latency.
*   -> The SSICs of pallets are needed to generate carton serial IDs; generated pallet IDs are placed in a blocking queue.
*   -> Additional workers (carton generators/inserters) consume from that queue and perform the same generate-and-insert pattern.
*   -> Poison pills are used to terminate the producer-consumer loops.
*
*   BENCHMARKING
*   -> Achieved ~7 seconds for 5.51 million labels hashed and inserted into PostgreSQL.
*   -> Original SLA: 10,000 labels in under 5 seconds.
*   -> Further optimization is planned to meet the SLA.
*
*   REMAINING CONSIDERATIONS
*   -> Trade-offs: MAC cloning (warm start for hashing) vs. full hashing without warm start.
*   -> Whether binary protocol is necessary at current performance levels remains an open question.
*   -> Byte array copies in the LabelGenerator class should be reviewed for potential optimization.
* 

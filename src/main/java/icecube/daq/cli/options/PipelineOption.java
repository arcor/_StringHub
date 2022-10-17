package icecube.daq.cli.options;

import icecube.daq.cli.filter.Filter;
import icecube.daq.cli.transform.Transform;
import icecube.daq.cli.stream.RecordType;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

/**
 * Aggregates multiple filter and transform options into a
 * record stream processing pipeline
 */
public class PipelineOption
{

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..*")
    private List<PipelineStageOption> operations;

    public String describe()
    {
        int n=0;
        StringBuilder sb = new StringBuilder();
        for(PipelineStageOption op: operations)
        {
            sb.append(String.format("[%d] : %s%n", n++, op.describe()));
        }
        return sb.toString();
    }

    public StreamTail plumb(StreamTail stream)
    {
        StreamTail tail = stream;
        for (PipelineStageOption op : operations) {
            tail = op.plumb(tail);
        }
        return tail;
    }

    static class PipelineStageOption
    {
        boolean populated = false;

        private Filter filter;
        private Transform transform;


        @CommandLine.Option(names={"--filter", "-f"}, required = false,
                paramLabel = "FILTER_SPEC",
                description = "A filter operation to apply. Multiple filter/transform arguments will be applied serially%n%n" +
                        "FILTER_SPEC: [${COMPLETION-CANDIDATES}]%n",
                completionCandidates = FilterOption.FilterCompletions.class,
                converter = FilterOption.class)
        public void setFilter(Filter filter)
        {
            ensureExclusive();

            this.filter = filter;
            populated = true;
        }

        @CommandLine.Option(names={"--transform", "-t"}, required = false,
                paramLabel = "TRANSFORM_SPEC",
                description = "A transform operation to apply. Multiple filter/transform arguments will be applied serially%n%n" +
                        "TRANSFORM_SPEC: [${COMPLETION-CANDIDATES}]%n",
                completionCandidates = TransformOption.TransformerCompletions.class,
                converter = TransformOption.class)
        public void setTransform(Transform transform)
        {
            ensureExclusive();

            this.transform = transform;
            populated = true;
        }

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        // verify the "exclusive" policy which is required to preserve operations ordering
        void ensureExclusive() throws CommandLine.ParameterException
        {
            if(populated)
            {
                throw new CommandLine.ParameterException(spec.commandLine(), "Mis-coded use of PipelineStageOption?");
            }
        }


        String describe()
        {
            if(filter != null)
            {
                return ("filter: " + filter.describe());

            }
            else if(transform != null)
            {
                return ("transform: " + transform.describe());
            }
            else {
                return "MISCODE:???";
            }
        }

        StreamTail plumb(StreamTail stream)
        {
            if(filter != null)
            {
                Stream<ByteBuffer> out = stream.stream.filter(filter.asPredicate(stream.outType));
                RecordType outType = stream.outType;
                return new StreamTail(out, outType);

            }
            else if(transform != null)
            {
                Stream<ByteBuffer> out = stream.stream.map(transform.asMapper(stream.outType));
                RecordType outType = transform.outputType(stream.outType);
                return new StreamTail(out, outType);
            }
            else {
               throw new Error("Miscode? Unpopulated PipelineStage");
            }
        }
    }

    /**
     * Models the state of the stream pipeline
     */
    public static class StreamTail
    {
        public final Stream<ByteBuffer> stream;
        public final RecordType outType;

        public StreamTail(Stream<ByteBuffer> stream, RecordType outType)
        {
            this.stream = stream;
            this.outType = outType;
        }
    }

}

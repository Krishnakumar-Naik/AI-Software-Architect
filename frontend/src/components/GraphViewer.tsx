import React, { useEffect, useRef, useState } from 'react';
import cytoscape from 'cytoscape';
import { 
  Download, 
  Maximize2, 
  Search, 
  RefreshCw, 
  Layers 
} from 'lucide-react';

interface GraphViewerProps {
  graphData: {
    nodes: Array<{
      data: {
        id: string;
        label: string;
        type: string;
        metadata: Record<string, any>;
      };
    }>;
    edges: Array<{
      data: {
        id: string;
        source: string;
        target: string;
        relationshipType: string;
      };
    }>;
  };
  onSelectNode: (nodeData: any) => void;
}

const GraphViewer: React.FC<GraphViewerProps> = ({ graphData, onSelectNode }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const cyRef = useRef<cytoscape.Core | null>(null);
  const [layoutName, setLayoutName] = useState<'cose' | 'breadthfirst' | 'circle' | 'grid'>('cose');
  const [searchQuery, setSearchQuery] = useState('');

  // Node Color Maps
  const getNodeColor = (type: string) => {
    switch (type.toUpperCase()) {
      case 'CONTROLLER':
        return '#6366f1'; // Indigo
      case 'SERVICE':
        return '#10b981'; // Emerald
      case 'REPOSITORY':
        return '#f59e0b'; // Amber
      case 'INTERFACE':
        return '#06b6d4'; // Cyan
      case 'CLASS':
        return '#94a3b8'; // Slate
      case 'ENDPOINT':
        return '#f43f5e'; // Rose
      default:
        return '#64748b'; // Slate gray
    }
  };

  useEffect(() => {
    if (!containerRef.current || !graphData) return;

    // Destroy existing instance if any
    if (cyRef.current) {
      cyRef.current.destroy();
    }

    const cyElements: any[] = [];
    
    // Add nodes
    graphData.nodes.forEach(n => {
      cyElements.push({
        group: 'nodes',
        data: {
          id: n.data.id,
          label: n.data.label,
          type: n.data.type,
          metadata: n.data.metadata
        }
      });
    });

    // Add edges
    graphData.edges.forEach(e => {
      cyElements.push({
        group: 'edges',
        data: {
          id: e.data.id,
          source: e.data.source,
          target: e.data.target,
          relationshipType: e.data.relationshipType
        }
      });
    });

    // Initialize Cytoscape
    const cy = cytoscape({
      container: containerRef.current,
      elements: cyElements,
      style: [
        {
          selector: 'node',
          style: {
            'label': 'data(label)',
            'background-color': (node: any) => getNodeColor(node.data('type')),
            'color': '#f8fafc',
            'font-size': '10px',
            'font-family': 'monospace',
            'text-valign': 'center',
            'text-halign': 'center',
            'width': (node: any) => node.data('type') === 'ENDPOINT' ? '45px' : '65px',
            'height': (node: any) => node.data('type') === 'ENDPOINT' ? '45px' : '65px',
            'text-wrap': 'ellipsis',
            'text-max-width': '60px',
            'border-width': '2px',
            'border-color': '#1e293b',
            'transition-property': 'background-color, opacity, width, height',
            'transition-duration': 0.35
          }
        },
        {
          selector: 'edge',
          style: {
            'width': 2,
            'line-color': '#475569',
            'target-arrow-color': '#475569',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            'label': 'data(relationshipType)',
            'font-size': '7px',
            'font-family': 'sans-serif',
            'color': '#94a3b8',
            'text-background-opacity': 0.8,
            'text-background-color': '#0f172a',
            'text-background-padding': '2px',
            'text-background-shape': 'roundrectangle',
            'arrow-scale': 1.1,
            'transition-property': 'line-color, target-arrow-color, opacity',
            'transition-duration': 0.35
          }
        },
        {
          selector: 'edge[relationshipType="EXPOSES_ENDPOINT"]',
          style: {
            'line-style': 'dashed',
            'line-color': '#f43f5e',
            'target-arrow-color': '#f43f5e'
          }
        },
        {
          selector: 'edge[relationshipType="CALLS"]',
          style: {
            'line-color': '#818cf8',
            'target-arrow-color': '#818cf8'
          }
        },
        {
          selector: 'node:selected',
          style: {
            'border-width': '4px',
            'border-color': '#a5b4fc',
            'width': (node: any) => node.data('type') === 'ENDPOINT' ? '52px' : '72px',
            'height': (node: any) => node.data('type') === 'ENDPOINT' ? '52px' : '72px',
          }
        }
      ],
      layout: {
        name: layoutName,
        fit: true,
        padding: 30,
        animate: true,
        animationDuration: 500
      } as any
    });

    // Handle node selection callback
    cy.on('tap', 'node', (evt) => {
      const node = evt.target;
      onSelectNode({
        id: node.data('id'),
        label: node.data('label'),
        type: node.data('type'),
        metadata: node.data('metadata')
      });
    });

    cyRef.current = cy;

    return () => {
      if (cyRef.current) {
        cyRef.current.destroy();
        cyRef.current = null;
      }
    };
  }, [graphData, layoutName]);

  // Handle Graph Search Highlighting & Neighbors Focus
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const cy = cyRef.current;
    if (!cy) return;

    if (!searchQuery.trim()) {
      // Clear filters, return to full visibility
      cy.elements().removeClass('dimmed highlighted');
      cy.fit();
      return;
    }

    const query = searchQuery.trim().toLowerCase();
    
    // Find matching node
    const matchingNode = cy.nodes().filter(n => {
      return n.data('id').toLowerCase().includes(query) || 
             (n.data('label') && n.data('label').toLowerCase().includes(query));
    });

    if (matchingNode.length > 0) {
      const target = matchingNode.first();
      
      // Get neighbors and connecting edges
      const neighbors = target.neighborhood();
      const connectedElems = target.union(neighbors);

      // Add highlighted classes, dim others
      cy.elements().removeClass('highlighted').addClass('dimmed');
      connectedElems.removeClass('dimmed').addClass('highlighted');
      target.select();

      // Apply stylesheet-level rules for dimmed/highlighted states
      cy.style()
        .selector('.dimmed')
        .style({
          'opacity': 0.18,
          'text-background-opacity': 0.0
        })
        .selector('.highlighted')
        .style({
          'opacity': 1.0,
          'text-background-opacity': 0.8
        })
        .update();

      // Zoom to target node
      cy.animate({
        center: { eles: target },
        zoom: 1.5,
        duration: 500
      });

      // Trigger select node callback for details view
      onSelectNode({
        id: target.data('id'),
        label: target.data('label'),
        type: target.data('type'),
        metadata: target.data('metadata')
      });
    }
  };

  const handleReset = () => {
    setSearchQuery('');
    const cy = cyRef.current;
    if (cy) {
      cy.elements().removeClass('dimmed highlighted');
      // Reset stylesheet opacity
      cy.style()
        .selector('.dimmed').style('opacity', 1.0)
        .selector('.highlighted').style('opacity', 1.0)
        .update();
      cy.fit();
    }
  };

  // Export Canvas Actions
  const exportAsPng = () => {
    const cy = cyRef.current;
    if (!cy) return;
    const png64 = cy.png({
      bg: '#090d26',
      full: true
    });
    
    const link = document.createElement('a');
    link.href = png64;
    link.download = `architecture-graph-${new Date().getTime()}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportAsSvg = () => {
    const cy = cyRef.current;
    if (!cy) return;
    
    // Cytoscape does not natively produce an SVG file directly from cy.svg() without extensions,
    // but we can wrap the canvas image inside a basic XML SVG or output a higher res PNG.
    // To support a clean SVG placeholder download, we export a high-quality SVG container holding the base64 png
    // which renders losslessly inside standard browsers:
    const png64 = cy.png({ bg: '#090d26', full: true });
    
    const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" width="${cy.width()}" height="${cy.height()}">
      <rect width="100%" height="100%" fill="#090d26"/>
      <image href="${png64}" width="100%" height="100%"/>
    </svg>`;
    
    const blob = new Blob([svgContent], { type: 'image/svg+xml;charset=utf-8' });
    const blobUrl = URL.createObjectURL(blob);
    
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = `architecture-graph-${new Date().getTime()}.svg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(blobUrl);
  };

  return (
    <div className="flex flex-col h-full w-full relative">
      {/* Top Toolbar */}
      <div className="flex items-center justify-between p-4 border-b border-border bg-card/40 backdrop-blur-md z-10">
        {/* Layout Selector */}
        <div className="flex items-center gap-2">
          <Layers className="h-4 w-4 text-primary" />
          <span className="text-xs font-semibold text-gray-400">Layout:</span>
          <select
            value={layoutName}
            onChange={(e) => setLayoutName(e.target.value as any)}
            className="px-2.5 py-1.5 rounded-lg border border-border bg-card text-xs text-white focus:outline-none focus:ring-1 focus:ring-primary/50 cursor-pointer"
          >
            <option value="cose">COSE (Force-Directed)</option>
            <option value="breadthfirst">Breadthfirst (Layered)</option>
            <option value="circle">Circle</option>
            <option value="grid">Grid</option>
          </select>
        </div>

        {/* Search */}
        <form onSubmit={handleSearch} className="flex items-center gap-2 max-w-sm w-full">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-500" />
            <input
              type="text"
              placeholder="Search components (e.g. UserService)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-lg border border-border bg-card/65 text-xs text-white placeholder-gray-600 focus:outline-none focus:ring-1 focus:ring-primary/50"
            />
          </div>
          <button
            type="submit"
            className="px-3 py-2 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-xs transition"
          >
            Search
          </button>
          {searchQuery && (
            <button
              type="button"
              onClick={handleReset}
              className="p-2 rounded-lg border border-border bg-card/45 hover:bg-card text-xs text-gray-400 hover:text-white transition"
              title="Reset Filters"
            >
              <RefreshCw className="h-3.5 w-3.5" />
            </button>
          )}
        </form>

        {/* Actions */}
        <div className="flex items-center gap-2">
          <button
            onClick={exportAsPng}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border bg-card/55 hover:bg-card text-xs text-gray-300 hover:text-white transition"
          >
            <Download className="h-3.5 w-3.5" /> Export PNG
          </button>
          <button
            onClick={exportAsSvg}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border bg-card/55 hover:bg-card text-xs text-gray-300 hover:text-white transition"
          >
            <Download className="h-3.5 w-3.5" /> Export SVG
          </button>
          <button
            onClick={() => cyRef.current && cyRef.current.fit()}
            className="p-2 rounded-lg border border-border bg-card/55 hover:bg-card text-xs text-gray-300 hover:text-white transition"
            title="Fit Graph to Screen"
          >
            <Maximize2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Graph Canvas Container */}
      <div 
        ref={containerRef} 
        className="flex-1 bg-[#090d26] select-none"
        style={{ height: 'calc(100% - 60px)' }}
      />
    </div>
  );
};

export default GraphViewer;
